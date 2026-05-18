package com.stablecoin.custody.fireblocks

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import com.jayway.jsonpath.JsonPath
import com.stablecoin.custody.fireblocks.domain.audit.AuditLogRepository
import com.stablecoin.custody.fireblocks.domain.event.AddressCreatedEvent
import com.stablecoin.custody.fireblocks.domain.event.VaultCreatedEvent
import com.stablecoin.custody.fireblocks.domain.event.WalletAssetCreatedEvent
import com.stablecoin.custody.fireblocks.test.containers.SharedTestContainers
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.Duration
import java.util.UUID

class VaultLifecycleBusinessTest : AbstractBusinessTest() {
    @Autowired
    private lateinit var auditLogRepository: AuditLogRepository

    @BeforeEach
    fun resetWireMock() {
        wireMock.resetAll()
    }

    private fun writeJwt() = jwt().jwt { it.subject(UUID.randomUUID().toString()).claim("scope", "custody:write") }

    private fun readJwt() = jwt().jwt { it.subject(UUID.randomUUID().toString()).claim("scope", "custody:read") }

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
    fun `should create vault, activate asset, generate deposit address, and query balance`() {
        // given
        val customerRefId = "biz-test-${UUID.randomUUID()}"
        val fireblocksVaultId = "fb-biz-001"

        wireMock.stubFor(
            WireMock
                .post(urlPathEqualTo("/v1/vault/accounts"))
                .willReturn(
                    WireMock
                        .aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("""{"id":"$fireblocksVaultId","name":"Lifecycle Vault","customerRefId":"$customerRefId"}"""),
                ),
        )

        // when — create vault
        val vaultResult =
            mockMvc
                .perform(
                    post("/api/v1/vaults")
                        .with(writeJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""{"customerRefId":"$customerRefId","name":"Lifecycle Vault"}"""),
                ).andExpect(status().isCreated)
                .andExpect(jsonPath("$.fireblocksVaultId").value(fireblocksVaultId))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andReturn()

        val vaultId = JsonPath.read<String>(vaultResult.response.contentAsString, "$.id")

        // then — verify vault audit log
        await().atMost(Duration.ofSeconds(5)).untilAsserted {
            assertThat(auditLogRepository.findByResourceId(vaultId)).isNotEmpty()
        }

        // given — activate asset
        wireMock.stubFor(
            WireMock
                .post(urlPathEqualTo("/v1/vault/accounts/$fireblocksVaultId/ETH"))
                .willReturn(
                    WireMock
                        .aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("""{"id":"ETH","available":"0"}"""),
                ),
        )

        // when — activate asset
        val assetResult =
            mockMvc
                .perform(
                    post("/api/v1/vaults/$vaultId/assets")
                        .with(writeJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""{"currency":"ETH","protocol":"ETH"}"""),
                ).andExpect(status().isCreated)
                .andExpect(jsonPath("$.currency").value("ETH"))
                .andExpect(jsonPath("$.protocol").value("ETH"))
                .andExpect(jsonPath("$.fireblocksAssetId").value("ETH"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andReturn()

        val walletAssetId = JsonPath.read<String>(assetResult.response.contentAsString, "$.id")

        // then — verify asset audit log
        await().atMost(Duration.ofSeconds(5)).untilAsserted {
            assertThat(auditLogRepository.findByResourceId(walletAssetId)).isNotEmpty()
        }

        // given — generate address
        wireMock.stubFor(
            WireMock
                .post(urlPathEqualTo("/v1/vault/accounts/$fireblocksVaultId/ETH/addresses"))
                .willReturn(
                    WireMock
                        .aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("""{"address":"0xbiz0001deadbeef","tag":null,"legacyAddress":null}"""),
                ),
        )

        // when — generate address
        mockMvc
            .perform(
                post("/api/v1/vaults/$vaultId/assets/ETH/ETH/addresses")
                    .with(writeJwt()),
            ).andExpect(status().isCreated)
            .andExpect(jsonPath("$.address").value("0xbiz0001deadbeef"))
            .andExpect(jsonPath("$.walletAssetId").value(walletAssetId))

        // given — query balance
        wireMock.stubFor(
            WireMock
                .post(urlPathEqualTo("/v1/vault/accounts/$fireblocksVaultId/ETH/balance"))
                .willReturn(
                    WireMock
                        .aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("""{"id":"ETH","total":"10.5","available":"8.0","pending":"2.5","frozen":"0","locked":"0"}"""),
                ),
        )

        // when / then — query balance
        mockMvc
            .perform(
                get("/api/v1/vaults/$vaultId/assets/ETH/ETH/balance")
                    .with(readJwt()),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.total").value(10.5))
            .andExpect(jsonPath("$.available").value(8.0))
            .andExpect(jsonPath("$.pending").value(2.5))

        // then — verify events published to Kafka
        assertKafkaTopicContains(VaultCreatedEvent.TOPIC, customerRefId)
        assertKafkaTopicContains(WalletAssetCreatedEvent.TOPIC, vaultId)
        assertKafkaTopicContains(AddressCreatedEvent.TOPIC, "0xbiz0001deadbeef")
    }

    @Test
    fun `should return 409 on duplicate vault creation with same customerRefId`() {
        // given
        val customerRefId = "dup-biz-${UUID.randomUUID()}"
        wireMock.stubFor(
            WireMock
                .post(urlPathEqualTo("/v1/vault/accounts"))
                .willReturn(
                    WireMock
                        .aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("""{"id":"fb-dup-biz-001","name":"Dup Vault","customerRefId":"$customerRefId"}"""),
                ),
        )

        mockMvc
            .perform(
                post("/api/v1/vaults")
                    .with(writeJwt())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"customerRefId":"$customerRefId","name":"Dup Vault"}"""),
            ).andExpect(status().isCreated)

        // when / then
        mockMvc
            .perform(
                post("/api/v1/vaults")
                    .with(writeJwt())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"customerRefId":"$customerRefId","name":"Dup Vault"}"""),
            ).andExpect(status().isConflict)
            .andExpect(jsonPath("$.code").value("CUSTODY-1002"))

        // then — verify Fireblocks was called only once
        wireMock.verify(1, postRequestedFor(urlPathEqualTo("/v1/vault/accounts")))
    }

    @Test
    fun `should activate all supported assets`() {
        // given
        val customerRefId = "multi-asset-${UUID.randomUUID()}"
        val fireblocksVaultId = "fb-multi-001"

        wireMock.stubFor(
            WireMock
                .post(urlPathEqualTo("/v1/vault/accounts"))
                .willReturn(
                    WireMock
                        .aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("""{"id":"$fireblocksVaultId","name":"Multi Asset Vault","customerRefId":"$customerRefId"}"""),
                ),
        )

        val vaultResult =
            mockMvc
                .perform(
                    post("/api/v1/vaults")
                        .with(writeJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""{"customerRefId":"$customerRefId","name":"Multi Asset Vault"}"""),
                ).andExpect(status().isCreated)
                .andReturn()

        val vaultId = JsonPath.read<String>(vaultResult.response.contentAsString, "$.id")

        val assets =
            listOf(
                Triple("BTC", "BTC", "BTC"),
                Triple("ETH", "ETH", "ETH"),
                Triple("SOL", "SOL", "SOL"),
                Triple("EURC", "ETH", "EURC"),
            )

        // when / then
        for ((currency, protocol, fireblocksAssetId) in assets) {
            wireMock.stubFor(
                WireMock
                    .post(urlPathEqualTo("/v1/vault/accounts/$fireblocksVaultId/$fireblocksAssetId"))
                    .willReturn(
                        WireMock
                            .aResponse()
                            .withStatus(200)
                            .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                            .withBody("""{"id":"$fireblocksAssetId","available":"0"}"""),
                    ),
            )

            mockMvc
                .perform(
                    post("/api/v1/vaults/$vaultId/assets")
                        .with(writeJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""{"currency":"$currency","protocol":"$protocol"}"""),
                ).andExpect(status().isCreated)
                .andExpect(jsonPath("$.currency").value(currency))
                .andExpect(jsonPath("$.protocol").value(protocol))
                .andExpect(jsonPath("$.fireblocksAssetId").value(fireblocksAssetId))
        }
    }

    @Test
    fun `should return existing asset on duplicate activation`() {
        // given
        val customerRefId = "dup-asset-${UUID.randomUUID()}"
        val fireblocksVaultId = "fb-dup-asset-001"

        wireMock.stubFor(
            WireMock
                .post(urlPathEqualTo("/v1/vault/accounts"))
                .willReturn(
                    WireMock
                        .aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("""{"id":"$fireblocksVaultId","name":"Dup Asset Vault","customerRefId":"$customerRefId"}"""),
                ),
        )

        val vaultResult =
            mockMvc
                .perform(
                    post("/api/v1/vaults")
                        .with(writeJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""{"customerRefId":"$customerRefId","name":"Dup Asset Vault"}"""),
                ).andExpect(status().isCreated)
                .andReturn()

        val vaultId = JsonPath.read<String>(vaultResult.response.contentAsString, "$.id")

        wireMock.stubFor(
            WireMock
                .post(urlPathEqualTo("/v1/vault/accounts/$fireblocksVaultId/BTC"))
                .willReturn(
                    WireMock
                        .aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("""{"id":"BTC","available":"0"}"""),
                ),
        )

        val firstResult =
            mockMvc
                .perform(
                    post("/api/v1/vaults/$vaultId/assets")
                        .with(writeJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""{"currency":"BTC","protocol":"BTC"}"""),
                ).andExpect(status().isCreated)
                .andReturn()

        val firstAssetId = JsonPath.read<String>(firstResult.response.contentAsString, "$.id")

        // when
        val secondResult =
            mockMvc
                .perform(
                    post("/api/v1/vaults/$vaultId/assets")
                        .with(writeJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""{"currency":"BTC","protocol":"BTC"}"""),
                ).andExpect(status().isCreated)
                .andReturn()

        val secondAssetId = JsonPath.read<String>(secondResult.response.contentAsString, "$.id")

        // then
        assertThat(secondAssetId).isEqualTo(firstAssetId)
        wireMock.verify(1, postRequestedFor(urlPathEqualTo("/v1/vault/accounts/$fireblocksVaultId/BTC")))
    }

    @Test
    fun `should return existing address on duplicate address generation`() {
        // given
        val customerRefId = "dup-addr-${UUID.randomUUID()}"
        val fireblocksVaultId = "fb-dup-addr-001"

        wireMock.stubFor(
            WireMock
                .post(urlPathEqualTo("/v1/vault/accounts"))
                .willReturn(
                    WireMock
                        .aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("""{"id":"$fireblocksVaultId","name":"Dup Addr Vault","customerRefId":"$customerRefId"}"""),
                ),
        )

        val vaultResult =
            mockMvc
                .perform(
                    post("/api/v1/vaults")
                        .with(writeJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""{"customerRefId":"$customerRefId","name":"Dup Addr Vault"}"""),
                ).andExpect(status().isCreated)
                .andReturn()

        val vaultId = JsonPath.read<String>(vaultResult.response.contentAsString, "$.id")

        wireMock.stubFor(
            WireMock
                .post(urlPathEqualTo("/v1/vault/accounts/$fireblocksVaultId/SOL"))
                .willReturn(
                    WireMock
                        .aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("""{"id":"SOL","available":"0"}"""),
                ),
        )

        mockMvc
            .perform(
                post("/api/v1/vaults/$vaultId/assets")
                    .with(writeJwt())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"currency":"SOL","protocol":"SOL"}"""),
            ).andExpect(status().isCreated)

        wireMock.stubFor(
            WireMock
                .post(urlPathEqualTo("/v1/vault/accounts/$fireblocksVaultId/SOL/addresses"))
                .willReturn(
                    WireMock
                        .aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("""{"address":"sol-addr-001","tag":null,"legacyAddress":null}"""),
                ),
        )

        val firstAddr =
            mockMvc
                .perform(
                    post("/api/v1/vaults/$vaultId/assets/SOL/SOL/addresses")
                        .with(writeJwt()),
                ).andExpect(status().isCreated)
                .andReturn()

        val firstAddress = JsonPath.read<String>(firstAddr.response.contentAsString, "$.address")

        // when
        val secondAddr =
            mockMvc
                .perform(
                    post("/api/v1/vaults/$vaultId/assets/SOL/SOL/addresses")
                        .with(writeJwt()),
                ).andExpect(status().isCreated)
                .andReturn()

        val secondAddress = JsonPath.read<String>(secondAddr.response.contentAsString, "$.address")

        // then
        assertThat(secondAddress).isEqualTo(firstAddress)
        wireMock.verify(1, postRequestedFor(urlPathEqualTo("/v1/vault/accounts/$fireblocksVaultId/SOL/addresses")))
    }

    @Test
    fun `should query balance with refresh=true`() {
        // given
        val customerRefId = "refresh-bal-${UUID.randomUUID()}"
        val fireblocksVaultId = "fb-refresh-001"

        wireMock.stubFor(
            WireMock
                .post(urlPathEqualTo("/v1/vault/accounts"))
                .willReturn(
                    WireMock
                        .aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("""{"id":"$fireblocksVaultId","name":"Refresh Vault","customerRefId":"$customerRefId"}"""),
                ),
        )

        val vaultResult =
            mockMvc
                .perform(
                    post("/api/v1/vaults")
                        .with(writeJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""{"customerRefId":"$customerRefId","name":"Refresh Vault"}"""),
                ).andExpect(status().isCreated)
                .andReturn()

        val vaultId = JsonPath.read<String>(vaultResult.response.contentAsString, "$.id")

        wireMock.stubFor(
            WireMock
                .post(urlPathEqualTo("/v1/vault/accounts/$fireblocksVaultId/EURC/balance"))
                .willReturn(
                    WireMock
                        .aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("""{"id":"EURC","total":"250.00","available":"200.00","pending":"50.00","frozen":"0","locked":"0"}"""),
                ),
        )

        // when / then
        mockMvc
            .perform(
                get("/api/v1/vaults/$vaultId/assets/EURC/ETH/balance?refresh=true")
                    .with(readJwt()),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.total").value(250.00))
            .andExpect(jsonPath("$.available").value(200.00))
            .andExpect(jsonPath("$.pending").value(50.00))
            .andExpect(jsonPath("$.frozen").value(0))
            .andExpect(jsonPath("$.locked").value(0))
    }
}
