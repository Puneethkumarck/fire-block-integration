package com.stablecoin.custody.fireblocks

import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import com.github.tomakehurst.wiremock.stubbing.Scenario
import com.jayway.jsonpath.JsonPath
import com.stablecoin.custody.fireblocks.domain.vault.Vault
import com.stablecoin.custody.fireblocks.domain.vault.VaultId
import com.stablecoin.custody.fireblocks.domain.vault.VaultRepository
import com.stablecoin.custody.fireblocks.domain.vault.VaultStatus
import com.stablecoin.custody.fireblocks.infrastructure.scheduling.VaultRecoveryJob
import com.stablecoin.custody.fireblocks.test.containers.SharedTestContainers
import com.stablecoin.custody.fireblocks.test.fixtures.signWebhookBody
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import org.springframework.test.util.AopTestUtils
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

class EdgeCasesBusinessTest : AbstractBusinessTest() {
    @Autowired
    private lateinit var vaultRepository: VaultRepository

    @Autowired
    private lateinit var vaultRecoveryJob: VaultRecoveryJob

    @Autowired
    private lateinit var circuitBreakerRegistry: CircuitBreakerRegistry

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    private lateinit var vaultId: String
    private val fireblocksVaultId = "fb-edge-001"

    @BeforeEach
    fun setUp() {
        wireMock.resetAll()
        circuitBreakerRegistry.allCircuitBreakers.forEach { it.reset() }
        vaultId = createVaultWithAsset()
        circuitBreakerRegistry.allCircuitBreakers.forEach { it.reset() }
        jdbcTemplate.update("DELETE FROM shedlock")
    }

    private fun writeJwt() = jwt().jwt { it.subject(UUID.randomUUID().toString()).claim("scope", "custody:write") }

    private fun readJwt() = jwt().jwt { it.subject(UUID.randomUUID().toString()).claim("scope", "custody:read") }

    private fun createVaultWithAsset(): String {
        val customerRefId = "edge-${UUID.randomUUID()}"

        wireMock.stubFor(
            post(urlPathEqualTo("/v1/vault/accounts"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("""{"id":"$fireblocksVaultId","name":"Edge Vault","customerRefId":"$customerRefId"}"""),
                ),
        )

        val vaultResult =
            mockMvc
                .perform(
                    MockMvcRequestBuilders
                        .post("/api/v1/vaults")
                        .with(writeJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""{"customerRefId":"$customerRefId","name":"Edge Vault"}"""),
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

    // ---------- Input Validation ----------

    @Test
    fun `should reject transaction with blank currency`() {
        // given
        val request =
            """{
            "externalTxId":"ext-${UUID.randomUUID()}",
            "sourceVaultId":"$vaultId",
            "destinationAddress":"0xabcdef1234567890abcdef1234567890abcdef12",
            "currency":"",
            "protocol":"ETH",
            "amount":"1.0"
        }"""

        // when / then
        mockMvc
            .perform(
                MockMvcRequestBuilders
                    .post("/api/v1/transactions")
                    .with(writeJwt())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(request),
            ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("CUSTODY-0001"))
            .andExpect(jsonPath("$.details.currency").exists())
    }

    @Test
    fun `should reject transaction with zero amount`() {
        // given
        val request =
            """{
            "externalTxId":"ext-${UUID.randomUUID()}",
            "sourceVaultId":"$vaultId",
            "destinationAddress":"0xabcdef1234567890abcdef1234567890abcdef12",
            "currency":"ETH",
            "protocol":"ETH",
            "amount":"0"
        }"""

        // when / then
        mockMvc
            .perform(
                MockMvcRequestBuilders
                    .post("/api/v1/transactions")
                    .with(writeJwt())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(request),
            ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("CUSTODY-0001"))
            .andExpect(jsonPath("$.details.amount").exists())
    }

    @Test
    fun `should reject transaction with negative amount`() {
        // given
        val request =
            """{
            "externalTxId":"ext-${UUID.randomUUID()}",
            "sourceVaultId":"$vaultId",
            "destinationAddress":"0xabcdef1234567890abcdef1234567890abcdef12",
            "currency":"ETH",
            "protocol":"ETH",
            "amount":"-5.0"
        }"""

        // when / then
        mockMvc
            .perform(
                MockMvcRequestBuilders
                    .post("/api/v1/transactions")
                    .with(writeJwt())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(request),
            ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("CUSTODY-0001"))
            .andExpect(jsonPath("$.details.amount").exists())
    }

    @Test
    fun `should reject transaction with blank destination address`() {
        // given
        val request =
            """{
            "externalTxId":"ext-${UUID.randomUUID()}",
            "sourceVaultId":"$vaultId",
            "destinationAddress":"",
            "currency":"ETH",
            "protocol":"ETH",
            "amount":"1.0"
        }"""

        // when / then
        mockMvc
            .perform(
                MockMvcRequestBuilders
                    .post("/api/v1/transactions")
                    .with(writeJwt())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(request),
            ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("CUSTODY-0001"))
            .andExpect(jsonPath("$.details.destinationAddress").exists())
    }

    @Test
    fun `should reject transaction with destination address exceeding max length`() {
        // given
        val longAddress = "0x" + "a".repeat(255)
        val request =
            """{
            "externalTxId":"ext-${UUID.randomUUID()}",
            "sourceVaultId":"$vaultId",
            "destinationAddress":"$longAddress",
            "currency":"ETH",
            "protocol":"ETH",
            "amount":"1.0"
        }"""

        // when / then
        mockMvc
            .perform(
                MockMvcRequestBuilders
                    .post("/api/v1/transactions")
                    .with(writeJwt())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(request),
            ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("CUSTODY-0001"))
            .andExpect(jsonPath("$.details.destinationAddress").exists())
    }

    @Test
    fun `should reject vault creation with blank customerRefId`() {
        // when / then
        mockMvc
            .perform(
                MockMvcRequestBuilders
                    .post("/api/v1/vaults")
                    .with(writeJwt())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"customerRefId":"","name":"Test Vault"}"""),
            ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("CUSTODY-0001"))
            .andExpect(jsonPath("$.details.customerRefId").exists())
    }

    @Test
    fun `should reject vault creation with name exceeding max length`() {
        // given
        val longName = "V".repeat(129)

        // when / then
        mockMvc
            .perform(
                MockMvcRequestBuilders
                    .post("/api/v1/vaults")
                    .with(writeJwt())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"customerRefId":"ref-${UUID.randomUUID()}","name":"$longName"}"""),
            ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("CUSTODY-0001"))
            .andExpect(jsonPath("$.details.name").exists())
    }

    @Test
    fun `should reject fee estimation for unsupported currency-protocol pair`() {
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
                        "currency":"DOGE",
                        "protocol":"DOGE",
                        "amount":"1.0"
                    }""",
                    ),
            ).andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("CUSTODY-1003"))
    }

    // ---------- Vault State ----------

    @Test
    fun `should reject transaction submission against pending vault`() {
        // given
        val pendingVault =
            vaultRepository.save(
                Vault(
                    id = VaultId(UUID.randomUUID()),
                    fireblocksVaultId = null,
                    customerRefId = "pending-tx-${UUID.randomUUID()}",
                    name = "Pending Vault",
                    status = VaultStatus.PENDING,
                    createdAt = Instant.now(),
                    updatedAt = Instant.now(),
                ),
            )

        // when / then
        mockMvc
            .perform(
                MockMvcRequestBuilders
                    .post("/api/v1/transactions")
                    .with(writeJwt())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """{
                        "externalTxId":"ext-${UUID.randomUUID()}",
                        "sourceVaultId":"${pendingVault.id.value}",
                        "destinationAddress":"0xabcdef1234567890abcdef1234567890abcdef12",
                        "currency":"ETH",
                        "protocol":"ETH",
                        "amount":"1.0"
                    }""",
                    ),
            ).andExpect(status().`is`(422))
            .andExpect(jsonPath("$.code").value("CUSTODY-1006"))
    }

    @Test
    fun `should reject fee estimation against pending vault`() {
        // given
        val pendingVault =
            vaultRepository.save(
                Vault(
                    id = VaultId(UUID.randomUUID()),
                    fireblocksVaultId = null,
                    customerRefId = "pending-fee-${UUID.randomUUID()}",
                    name = "Pending Fee Vault",
                    status = VaultStatus.PENDING,
                    createdAt = Instant.now(),
                    updatedAt = Instant.now(),
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
                        "sourceVaultId":"${pendingVault.id.value}",
                        "destinationAddress":"0xabcdef1234567890abcdef1234567890abcdef12",
                        "currency":"ETH",
                        "protocol":"ETH",
                        "amount":"1.0"
                    }""",
                    ),
            ).andExpect(status().`is`(422))
            .andExpect(jsonPath("$.code").value("CUSTODY-1006"))
    }

    // ---------- Security ----------

    @Test
    fun `should return 401 for unauthenticated API request`() {
        // when / then
        mockMvc
            .perform(
                MockMvcRequestBuilders
                    .get("/api/v1/transactions/ext-123"),
            ).andExpect(status().isUnauthorized)
    }

    @Test
    fun `should return 403 when scope is insufficient`() {
        // when / then
        mockMvc
            .perform(
                MockMvcRequestBuilders
                    .post("/api/v1/vaults")
                    .with(readJwt())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"customerRefId":"ref-001","name":"Test"}"""),
            ).andExpect(status().isForbidden)
    }

    @Test
    fun `should return 401 for webhook with invalid signature`() {
        // given
        val body =
            """{"type":"TRANSACTION_STATUS_UPDATED","createdAt":${Instant.now().toEpochMilli()},"data":{"id":"fb-bad-sig","status":"COMPLETED","subStatus":null,"txHash":null}}"""

        // when / then
        mockMvc
            .perform(
                MockMvcRequestBuilders
                    .post("/api/v1/webhooks/fireblocks")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Fireblocks-Signature", "aW52YWxpZC1zaWduYXR1cmU=")
                    .content(body),
            ).andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.code").value("CUSTODY-4001"))
    }

    @Test
    fun `should return 401 for webhook with expired timestamp`() {
        // given
        val expiredTimestamp = Instant.now().minus(10, ChronoUnit.MINUTES).toEpochMilli()
        val body =
            """{"type":"TRANSACTION_STATUS_UPDATED","createdAt":$expiredTimestamp,"data":{"id":"fb-expired","status":"COMPLETED","subStatus":null,"txHash":null}}"""
        val signature = signWebhookBody(body, SharedTestContainers.webhookKeyPair)

        // when / then
        mockMvc
            .perform(
                MockMvcRequestBuilders
                    .post("/api/v1/webhooks/fireblocks")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Fireblocks-Signature", signature)
                    .content(body),
            ).andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.code").value("CUSTODY-4001"))
    }

    // ---------- Error Response Structure ----------

    @Test
    fun `should not expose internal details in error responses`() {
        // given
        wireMock.stubFor(
            post(urlPathEqualTo("/v1/transactions/estimate_fee"))
                .willReturn(aResponse().withStatus(500).withBody("Internal Server Error")),
        )

        // when
        val result =
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
                ).andReturn()

        // then
        val body = result.response.contentAsString
        assertThat(body).doesNotContain("Exception")
        assertThat(body).doesNotContain("stackTrace")
        assertThat(body).doesNotContain("com.stablecoin")
        assertThat(body).doesNotContain("org.springframework")
        assertThat(JsonPath.read<String>(body, "$.code")).isNotBlank()
        assertThat(JsonPath.read<String>(body, "$.status")).isNotBlank()
        assertThat(JsonPath.read<String>(body, "$.message")).isNotBlank()
    }

    @Test
    fun `should return structured error with required fields for all error types`() {
        // when — validation error
        val validationResult =
            mockMvc
                .perform(
                    MockMvcRequestBuilders
                        .post("/api/v1/vaults")
                        .with(writeJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""{"customerRefId":"","name":""}"""),
                ).andExpect(status().isBadRequest)
                .andReturn()

        // then
        val validationBody = validationResult.response.contentAsString
        assertThat(JsonPath.read<String>(validationBody, "$.code")).isEqualTo("CUSTODY-0001")
        assertThat(JsonPath.read<String>(validationBody, "$.status")).isEqualTo("Bad Request")
        assertThat(JsonPath.read<String>(validationBody, "$.message")).isNotBlank()
        assertThat(JsonPath.read<Map<String, String>>(validationBody, "$.details")).isNotEmpty()
    }

    // ---------- Resilience ----------

    @Test
    fun `should open circuit breaker after consecutive failures and block subsequent calls`() {
        // given
        wireMock.stubFor(
            post(urlPathEqualTo("/v1/transactions"))
                .willReturn(aResponse().withStatus(500).withBody("""{"message":"Internal Server Error"}""")),
        )

        // when — trip the circuit breaker with 3 failures
        repeat(3) { i ->
            mockMvc
                .perform(
                    MockMvcRequestBuilders
                        .post("/api/v1/transactions")
                        .with(writeJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            """{
                            "externalTxId":"ext-cb-${UUID.randomUUID()}",
                            "sourceVaultId":"$vaultId",
                            "destinationAddress":"0xabcdef1234567890abcdef1234567890abcdef12",
                            "currency":"ETH",
                            "protocol":"ETH",
                            "amount":"1.0"
                        }""",
                        ),
                ).andExpect(status().isCreated)
        }

        // when — 4th call should be blocked by circuit breaker
        mockMvc
            .perform(
                MockMvcRequestBuilders
                    .post("/api/v1/transactions")
                    .with(writeJwt())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """{
                        "externalTxId":"ext-cb-blocked-${UUID.randomUUID()}",
                        "sourceVaultId":"$vaultId",
                        "destinationAddress":"0xabcdef1234567890abcdef1234567890abcdef12",
                        "currency":"ETH",
                        "protocol":"ETH",
                        "amount":"1.0"
                    }""",
                    ),
            ).andExpect(status().isCreated)

        // then — WireMock should have received exactly 3 calls, not 4
        wireMock.verify(3, postRequestedFor(urlPathEqualTo("/v1/transactions")))
    }

    @Test
    fun `should maintain separate circuit breaker for balance queries`() {
        // given — trip the main fireblocks circuit breaker
        wireMock.stubFor(
            post(urlPathEqualTo("/v1/transactions"))
                .willReturn(aResponse().withStatus(500).withBody("""{"message":"error"}""")),
        )

        repeat(3) {
            mockMvc
                .perform(
                    MockMvcRequestBuilders
                        .post("/api/v1/transactions")
                        .with(writeJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            """{
                            "externalTxId":"ext-sep-cb-${UUID.randomUUID()}",
                            "sourceVaultId":"$vaultId",
                            "destinationAddress":"0xabcdef1234567890abcdef1234567890abcdef12",
                            "currency":"ETH",
                            "protocol":"ETH",
                            "amount":"1.0"
                        }""",
                        ),
                ).andExpect(status().isCreated)
        }

        // given — stub balance endpoint
        wireMock.stubFor(
            post(urlPathEqualTo("/v1/vault/accounts/$fireblocksVaultId/ETH/balance"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("""{"id":"ETH","total":"5.0","available":"3.0","pending":"2.0","frozen":"0","locked":"0"}"""),
                ),
        )

        // when / then — balance should still work (separate circuit breaker)
        mockMvc
            .perform(
                MockMvcRequestBuilders
                    .get("/api/v1/vaults/$vaultId/assets/ETH/ETH/balance")
                    .with(readJwt()),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.total").value(5.0))
    }

    @Test
    fun `should retry on server error and succeed on subsequent attempt`() {
        // given — first 2 calls fail, third succeeds (maxAttempts=3)
        wireMock.stubFor(
            post(urlPathEqualTo("/v1/transactions/estimate_fee"))
                .inScenario("retry-test")
                .whenScenarioStateIs(Scenario.STARTED)
                .willReturn(aResponse().withStatus(500).withBody("""{"message":"error"}"""))
                .willSetStateTo("RETRY_1"),
        )
        wireMock.stubFor(
            post(urlPathEqualTo("/v1/transactions/estimate_fee"))
                .inScenario("retry-test")
                .whenScenarioStateIs("RETRY_1")
                .willReturn(aResponse().withStatus(500).withBody("""{"message":"error"}"""))
                .willSetStateTo("RETRY_2"),
        )
        wireMock.stubFor(
            post(urlPathEqualTo("/v1/transactions/estimate_fee"))
                .inScenario("retry-test")
                .whenScenarioStateIs("RETRY_2")
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

        // then — 3 calls total (1 original + 2 retries)
        wireMock.verify(3, postRequestedFor(urlPathEqualTo("/v1/transactions/estimate_fee")))
    }

    @Test
    fun `should not retry on client error`() {
        // given
        wireMock.stubFor(
            post(urlPathEqualTo("/v1/transactions/estimate_fee"))
                .willReturn(
                    aResponse()
                        .withStatus(400)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("""{"message":"Bad request from Fireblocks"}"""),
                ),
        )

        // when
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
            ).andReturn()

        // then — only 1 call (no retries for 4xx)
        wireMock.verify(1, postRequestedFor(urlPathEqualTo("/v1/transactions/estimate_fee")))
    }

    @Test
    fun `should return timeout error when upstream API is slow`() {
        // given
        wireMock.stubFor(
            post(urlPathEqualTo("/v1/transactions/estimate_fee"))
                .willReturn(
                    aResponse()
                        .withFixedDelay(10_000)
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody(
                            """{"low":{"networkFee":"0.0001","gasPrice":"10"},"medium":{"networkFee":"0.0005","gasPrice":"20"},"high":{"networkFee":"0.001","gasPrice":"30"}}""",
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
            ).andExpect(status().isGatewayTimeout)
            .andExpect(jsonPath("$.code").value("CUSTODY-3002"))
    }

    // ---------- Vault Recovery ----------

    @Test
    fun `should recover pending vault via recovery job`() {
        // given
        val pendingVault =
            vaultRepository.save(
                Vault(
                    id = VaultId(UUID.randomUUID()),
                    fireblocksVaultId = null,
                    customerRefId = "recovery-${UUID.randomUUID()}",
                    name = "Recovery Vault",
                    status = VaultStatus.PENDING,
                    createdAt = Instant.now().minus(10, ChronoUnit.MINUTES),
                    updatedAt = Instant.now().minus(10, ChronoUnit.MINUTES),
                ),
            )

        wireMock.stubFor(
            post(urlPathEqualTo("/v1/vault/accounts"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("""{"id":"fb-recovered-001","name":"Recovery Vault","customerRefId":"${pendingVault.customerRefId}"}"""),
                ),
        )

        // when — call target directly to bypass @SchedulerLock proxy
        AopTestUtils.getTargetObject<VaultRecoveryJob>(vaultRecoveryJob).recoverPendingVaults()

        // then
        val recovered = vaultRepository.findById(pendingVault.id)
        assertThat(recovered).isNotNull
        assertThat(recovered!!.status).isEqualTo(VaultStatus.ACTIVE)
        assertThat(recovered.fireblocksVaultId).isEqualTo("fb-recovered-001")
    }

    @Test
    fun `should mark vault as failed when recovery encounters error`() {
        // given
        val pendingVault =
            vaultRepository.save(
                Vault(
                    id = VaultId(UUID.randomUUID()),
                    fireblocksVaultId = "fb-recovery-404",
                    customerRefId = "fail-recovery-${UUID.randomUUID()}",
                    name = "Fail Recovery Vault",
                    status = VaultStatus.PENDING,
                    createdAt = Instant.now().minus(10, ChronoUnit.MINUTES),
                    updatedAt = Instant.now().minus(10, ChronoUnit.MINUTES),
                ),
            )

        wireMock.stubFor(
            get(urlPathEqualTo("/v1/vault/accounts/fb-recovery-404"))
                .willReturn(
                    aResponse()
                        .withStatus(404)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("""{"message":"Vault not found"}"""),
                ),
        )

        // when — call target directly to bypass @SchedulerLock proxy
        AopTestUtils.getTargetObject<VaultRecoveryJob>(vaultRecoveryJob).recoverPendingVaults()

        // then
        val failed = vaultRepository.findById(pendingVault.id)
        assertThat(failed).isNotNull
        assertThat(failed!!.status).isEqualTo(VaultStatus.FAILED)
    }
}
