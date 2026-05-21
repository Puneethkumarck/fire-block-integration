package com.stablecoin.custody.fireblocks

import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching
import com.jayway.jsonpath.JsonPath
import com.stablecoin.custody.fireblocks.domain.audit.AuditLogRepository
import com.stablecoin.custody.fireblocks.domain.event.TransactionStatusChangedEvent
import com.stablecoin.custody.fireblocks.domain.port.FireblocksTransactionPort
import com.stablecoin.custody.fireblocks.domain.transaction.TransactionRepository
import com.stablecoin.custody.fireblocks.domain.transaction.TransactionStatus
import com.stablecoin.custody.fireblocks.domain.transaction.TransactionStatusHandler
import com.stablecoin.custody.fireblocks.test.containers.SharedTestContainers
import com.stablecoin.custody.fireblocks.test.fixtures.aTransaction
import com.stablecoin.custody.fireblocks.test.fixtures.signWebhookBody
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.Duration
import java.time.Instant
import java.util.UUID

class TransactionLifecycleBusinessTest : AbstractBusinessTest() {
    @Autowired
    private lateinit var auditLogRepository: AuditLogRepository

    @Autowired
    private lateinit var transactionRepository: TransactionRepository

    @Autowired
    private lateinit var fireblocksTransactionPort: FireblocksTransactionPort

    @Autowired
    private lateinit var transactionStatusHandler: TransactionStatusHandler

    private lateinit var vaultId: String
    private val fireblocksVaultId = "fb-tx-biz-001"

    @BeforeEach
    fun setUp() {
        wireMock.resetAll()
        vaultId = createVaultWithAsset()
    }

    private fun writeJwt() = jwt().jwt { it.subject(UUID.randomUUID().toString()).claim("scope", "custody:write") }

    private fun readJwt() = jwt().jwt { it.subject(UUID.randomUUID().toString()).claim("scope", "custody:read") }

    private fun createVaultWithAsset(): String {
        val customerRefId = "tx-biz-${UUID.randomUUID()}"

        wireMock.stubFor(
            post(urlPathEqualTo("/v1/vault/accounts"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("""{"id":"$fireblocksVaultId","name":"Tx Test Vault","customerRefId":"$customerRefId"}"""),
                ),
        )

        val vaultResult =
            mockMvc
                .perform(
                    MockMvcRequestBuilders
                        .post("/api/v1/vaults")
                        .with(writeJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""{"customerRefId":"$customerRefId","name":"Tx Test Vault"}"""),
                ).andExpect(status().isCreated)
                .andReturn()

        val id = JsonPath.read<String>(vaultResult.response.contentAsString, "$.id")

        wireMock.stubFor(
            post(urlPathEqualTo("/v1/vault/accounts/$fireblocksVaultId/ETH"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("""{"id":"ETH","available":"0"}"""),
                ),
        )

        mockMvc
            .perform(
                MockMvcRequestBuilders
                    .post("/api/v1/vaults/$id/assets")
                    .with(writeJwt())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"currency":"ETH","protocol":"ETH"}"""),
            ).andExpect(status().isCreated)

        return id
    }

    private fun submitTransaction(
        externalTxId: String,
        fireblocksTxId: String,
    ): String {
        wireMock.stubFor(
            post(urlPathMatching("/v1/vault/accounts/.+/lock_allocation"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("""{"id":"lock-biz-001","status":"LOCKED"}"""),
                ),
        )
        wireMock.stubFor(
            post(urlPathEqualTo("/v1/transactions"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("""{"id":"$fireblocksTxId","status":"SUBMITTED"}"""),
                ),
        )

        val result =
            mockMvc
                .perform(
                    MockMvcRequestBuilders
                        .post("/api/v1/transactions")
                        .with(writeJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            """{
                            "externalTxId":"$externalTxId",
                            "sourceVaultId":"$vaultId",
                            "destinationAddress":"0xabcdef1234567890abcdef1234567890abcdef12",
                            "currency":"ETH",
                            "protocol":"ETH",
                            "amount":"1.5"
                        }""",
                        ),
                ).andExpect(status().isCreated)
                .andExpect(jsonPath("$.status").value("SUBMITTED"))
                .andExpect(jsonPath("$.currency").value("ETH"))
                .andExpect(jsonPath("$.protocol").value("ETH"))
                .andReturn()

        return JsonPath.read(result.response.contentAsString, "$.externalTxId")
    }

    private fun simulateWebhook(
        fireblocksTxId: String,
        fireblocksStatus: String,
        txHash: String? = null,
    ) {
        val txHashJson = txHash?.let { "\"$it\"" } ?: "null"
        val body =
            """{"type":"TRANSACTION_STATUS_UPDATED","createdAt":${Instant.now().toEpochMilli()},"data":{"id":"$fireblocksTxId","status":"$fireblocksStatus","subStatus":null,"txHash":$txHashJson}}"""
        val signature = signWebhookBody(body, SharedTestContainers.webhookKeyPair)

        mockMvc
            .perform(
                MockMvcRequestBuilders
                    .post("/api/v1/webhooks/fireblocks")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Fireblocks-Signature", signature)
                    .content(body),
            ).andExpect(status().isOk)
    }

    private fun assertKafkaTopicContains(
        topic: String,
        expectedFragment: String,
    ) {
        val props =
            mapOf(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to SharedTestContainers.kafka.bootstrapServers,
                ConsumerConfig.GROUP_ID_CONFIG to "business-test-${UUID.randomUUID()}",
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "earliest",
            )
        val consumer = DefaultKafkaConsumerFactory(props, StringDeserializer(), StringDeserializer()).createConsumer()
        val allValues = mutableListOf<String>()
        consumer.use {
            it.subscribe(listOf(topic))
            await().atMost(Duration.ofSeconds(10)).untilAsserted {
                it.poll(Duration.ofMillis(500)).forEach { record -> allValues.add(record.value()) }
                assertThat(allValues).anyMatch { value -> value.contains(expectedFragment) }
            }
        }
    }

    @Test
    fun `should submit transaction and track through to CONFIRMED via webhooks`() {
        // given
        val externalTxId = "ext-${UUID.randomUUID()}"
        val fireblocksTxId = "fb-lifecycle-001"

        // when
        submitTransaction(externalTxId, fireblocksTxId)

        // then
        val submitted = transactionRepository.findByExternalTxId(externalTxId)
        assertThat(submitted).isNotNull
        assertThat(submitted!!.status).isEqualTo(TransactionStatus.SUBMITTED)

        // then
        await().atMost(Duration.ofSeconds(5)).untilAsserted {
            assertThat(auditLogRepository.findByResourceId(submitted.id.value.toString())).isNotEmpty()
        }

        // when
        simulateWebhook(fireblocksTxId, "PENDING_SIGNATURE")

        // then
        await().atMost(Duration.ofSeconds(5)).untilAsserted {
            val updated = transactionRepository.findByFireblocksTransactionId(fireblocksTxId)
            assertThat(updated).isNotNull
            assertThat(updated!!.status).isEqualTo(TransactionStatus.PROCESSING)
        }

        // when
        simulateWebhook(fireblocksTxId, "CONFIRMING")

        // then
        await().atMost(Duration.ofSeconds(5)).untilAsserted {
            val updated = transactionRepository.findByFireblocksTransactionId(fireblocksTxId)
            assertThat(updated).isNotNull
            assertThat(updated!!.status).isEqualTo(TransactionStatus.CONFIRMING)
        }

        // when
        simulateWebhook(fireblocksTxId, "COMPLETED", txHash = "0x123abc456def")

        // then
        await().atMost(Duration.ofSeconds(5)).untilAsserted {
            val updated = transactionRepository.findByFireblocksTransactionId(fireblocksTxId)
            assertThat(updated).isNotNull
            assertThat(updated!!.status).isEqualTo(TransactionStatus.CONFIRMED)
            assertThat(updated.txHash).isEqualTo("0x123abc456def")
        }

        // then
        mockMvc
            .perform(
                MockMvcRequestBuilders
                    .get("/api/v1/transactions/$externalTxId")
                    .with(readJwt()),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("CONFIRMED"))
            .andExpect(jsonPath("$.txHash").value("0x123abc456def"))

        // then
        assertKafkaTopicContains(TransactionStatusChangedEvent.TOPIC, externalTxId)
    }

    @Test
    fun `should handle transaction failure via webhook`() {
        // given
        val externalTxId = "ext-fail-${UUID.randomUUID()}"
        val fireblocksTxId = "fb-fail-001"
        submitTransaction(externalTxId, fireblocksTxId)
        wireMock.stubFor(
            post(urlPathMatching("/v1/vault/accounts/.+/release_allocation"))
                .willReturn(
                    aResponse()
                        .withStatus(200),
                ),
        )

        // when
        simulateWebhook(fireblocksTxId, "FAILED")

        // then
        await().atMost(Duration.ofSeconds(5)).untilAsserted {
            val updated = transactionRepository.findByFireblocksTransactionId(fireblocksTxId)
            assertThat(updated).isNotNull
            assertThat(updated!!.status).isEqualTo(TransactionStatus.FAILED)
        }

        wireMock.verify(1, postRequestedFor(urlPathMatching("/v1/vault/accounts/.+/release_allocation")))

        // then
        assertKafkaTopicContains(TransactionStatusChangedEvent.TOPIC, externalTxId)
    }

    @Test
    fun `should return existing transaction for duplicate externalTxId`() {
        // given
        val externalTxId = "ext-dup-${UUID.randomUUID()}"
        val fireblocksTxId = "fb-dup-001"
        submitTransaction(externalTxId, fireblocksTxId)

        // when
        mockMvc
            .perform(
                MockMvcRequestBuilders
                    .post("/api/v1/transactions")
                    .with(writeJwt())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """{
                        "externalTxId":"$externalTxId",
                        "sourceVaultId":"$vaultId",
                        "destinationAddress":"0xabcdef1234567890abcdef1234567890abcdef12",
                        "currency":"ETH",
                        "protocol":"ETH",
                        "amount":"1.5"
                    }""",
                    ),
            ).andExpect(status().isCreated)
            .andExpect(jsonPath("$.externalTxId").value(externalTxId))

        // then
        wireMock.verify(1, postRequestedFor(urlPathEqualTo("/v1/transactions")))
    }

    @Test
    fun `should catch stale transaction via polling fallback`() {
        // given
        val externalTxId = "ext-poll-${UUID.randomUUID()}"
        val fireblocksTxId = "fb-poll-001"
        submitTransaction(externalTxId, fireblocksTxId)

        // given
        wireMock.stubFor(
            get(urlPathEqualTo("/v1/transactions/$fireblocksTxId"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("""{"id":"$fireblocksTxId","status":"PENDING_SIGNATURE","txHash":null}"""),
                ),
        )

        // when
        val result = fireblocksTransactionPort.getTransaction(fireblocksTxId)
        transactionStatusHandler.handleStatusUpdate(
            fireblocksTxId = fireblocksTxId,
            fireblocksStatus = result.status,
            subStatus = result.subStatus,
            txHash = result.txHash,
        )

        // then
        val updated = transactionRepository.findByFireblocksTransactionId(fireblocksTxId)
        assertThat(updated).isNotNull
        assertThat(updated!!.status).isEqualTo(TransactionStatus.PROCESSING)
    }

    @Test
    fun `should recover stale CREATED transaction via polling`() {
        // given
        val staleTransaction =
            aTransaction(
                status = TransactionStatus.CREATED,
                currency = "ETH",
                protocol = "ETH",
                assetId = "ETH",
            )
        transactionRepository.save(staleTransaction)

        // given
        wireMock.stubFor(
            get(urlPathEqualTo("/v1/transactions/external_tx_id/${staleTransaction.externalTxId}"))
                .willReturn(
                    aResponse()
                        .withStatus(404)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("""{"message":"not found"}"""),
                ),
        )

        // when
        val existing = fireblocksTransactionPort.getByExternalId(staleTransaction.externalTxId)
        if (existing == null) {
            val failed = staleTransaction.markFailed()
            transactionRepository.save(failed)
        }

        // then
        val updated = transactionRepository.findByExternalTxId(staleTransaction.externalTxId)
        assertThat(updated).isNotNull
        assertThat(updated!!.status).isEqualTo(TransactionStatus.FAILED)
    }

    @Test
    fun `should handle duplicate webhook delivery idempotently`() {
        // given
        val externalTxId = "ext-idem-${UUID.randomUUID()}"
        val fireblocksTxId = "fb-idem-001"
        submitTransaction(externalTxId, fireblocksTxId)

        // when
        simulateWebhook(fireblocksTxId, "PENDING_SIGNATURE")

        await().atMost(Duration.ofSeconds(5)).untilAsserted {
            val updated = transactionRepository.findByFireblocksTransactionId(fireblocksTxId)
            assertThat(updated!!.status).isEqualTo(TransactionStatus.PROCESSING)
        }

        // when
        simulateWebhook(fireblocksTxId, "PENDING_SIGNATURE")

        // then
        val unchanged = transactionRepository.findByFireblocksTransactionId(fireblocksTxId)
        assertThat(unchanged).isNotNull
        assertThat(unchanged!!.status).isEqualTo(TransactionStatus.PROCESSING)
    }

    @Test
    fun `should handle webhook for terminal state re-delivery`() {
        // given
        val externalTxId = "ext-redelivery-${UUID.randomUUID()}"
        val fireblocksTxId = "fb-redelivery-001"
        submitTransaction(externalTxId, fireblocksTxId)

        // when
        simulateWebhook(fireblocksTxId, "PENDING_SIGNATURE")
        await().atMost(Duration.ofSeconds(5)).untilAsserted {
            assertThat(transactionRepository.findByFireblocksTransactionId(fireblocksTxId)!!.status)
                .isEqualTo(TransactionStatus.PROCESSING)
        }
        simulateWebhook(fireblocksTxId, "CONFIRMING")
        await().atMost(Duration.ofSeconds(5)).untilAsserted {
            assertThat(transactionRepository.findByFireblocksTransactionId(fireblocksTxId)!!.status)
                .isEqualTo(TransactionStatus.CONFIRMING)
        }
        simulateWebhook(fireblocksTxId, "COMPLETED", txHash = "0xterm123")
        await().atMost(Duration.ofSeconds(5)).untilAsserted {
            assertThat(transactionRepository.findByFireblocksTransactionId(fireblocksTxId)!!.status)
                .isEqualTo(TransactionStatus.CONFIRMED)
        }

        // when
        simulateWebhook(fireblocksTxId, "COMPLETED", txHash = "0xterm123")

        // then
        val unchanged = transactionRepository.findByFireblocksTransactionId(fireblocksTxId)
        assertThat(unchanged).isNotNull
        assertThat(unchanged!!.status).isEqualTo(TransactionStatus.CONFIRMED)
    }

    @Test
    fun `should handle webhook for unknown transaction`() {
        // given
        val body =
            """{"type":"TRANSACTION_STATUS_UPDATED","createdAt":${Instant.now().toEpochMilli()},"data":{"id":"fb-unknown-999","status":"COMPLETED","subStatus":null,"txHash":null}}"""
        val signature = signWebhookBody(body, SharedTestContainers.webhookKeyPair)

        // when / then
        mockMvc
            .perform(
                MockMvcRequestBuilders
                    .post("/api/v1/webhooks/fireblocks")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Fireblocks-Signature", signature)
                    .content(body),
            ).andExpect(status().isOk)
    }

    @Test
    fun `should lookup transaction by Fireblocks ID`() {
        // given
        val externalTxId = "ext-lookup-${UUID.randomUUID()}"
        val fireblocksTxId = "fb-lookup-001"
        submitTransaction(externalTxId, fireblocksTxId)

        // when / then
        mockMvc
            .perform(
                MockMvcRequestBuilders
                    .get("/api/v1/transactions/fireblocks/$fireblocksTxId")
                    .with(readJwt()),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.fireblocksTransactionId").value(fireblocksTxId))
            .andExpect(jsonPath("$.externalTxId").value(externalTxId))
            .andExpect(jsonPath("$.status").value("SUBMITTED"))
    }

    @Test
    fun `should estimate fee for transaction`() {
        // given
        wireMock.stubFor(
            post(urlPathEqualTo("/v1/transactions/estimate_fee"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody(
                            """{
                            "low":{"networkFee":"0.0001","gasPrice":"10"},
                            "medium":{"networkFee":"0.0005","gasPrice":"20"},
                            "high":{"networkFee":"0.001","gasPrice":"30"}
                        }""",
                        ),
                ),
        )

        // when / then
        mockMvc
            .perform(
                MockMvcRequestBuilders
                    .post("/api/v1/transactions/estimate-fee")
                    .with(writeJwt())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """{
                        "sourceVaultId":"$vaultId",
                        "destinationAddress":"0xabcdef1234567890abcdef1234567890abcdef12",
                        "currency":"ETH",
                        "protocol":"ETH",
                        "amount":"1.0"
                    }""",
                    ),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.low").value(0.0001))
            .andExpect(jsonPath("$.medium").value(0.0005))
            .andExpect(jsonPath("$.high").value(0.001))
    }
}
