package com.stablecoin.custody.fireblocks.application.controller

import com.github.tomakehurst.wiremock.client.WireMock
import com.stablecoin.custody.fireblocks.AbstractMockMvcIntegrationTest
import com.stablecoin.custody.fireblocks.domain.transaction.TransactionRepository
import com.stablecoin.custody.fireblocks.domain.vault.VaultRepository
import com.stablecoin.custody.fireblocks.test.fixtures.aTransaction
import com.stablecoin.custody.fireblocks.test.fixtures.aVault
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.math.BigDecimal
import java.util.UUID

class TransactionControllerIntegrationTest : AbstractMockMvcIntegrationTest() {
    @Autowired
    private lateinit var vaultRepository: VaultRepository

    @Autowired
    private lateinit var transactionRepository: TransactionRepository

    @BeforeEach
    fun resetWireMock() {
        wireMock.resetAll()
    }

    private fun writeJwt() = jwt().jwt { it.subject(UUID.randomUUID().toString()).claim("scope", "custody:write") }

    private fun readJwt() = jwt().jwt { it.subject(UUID.randomUUID().toString()).claim("scope", "custody:read") }

    @Test
    fun `should submit transaction end-to-end`() {
        // given
        val vault = vaultRepository.save(aVault(fireblocksVaultId = "fb-tx-int-001"))
        val externalTxId = "int-ext-${UUID.randomUUID()}"
        wireMock.stubFor(
            WireMock
                .post(WireMock.urlPathEqualTo("/v1/transactions"))
                .willReturn(
                    WireMock
                        .aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody(
                            """{"id":"fb-created-tx-001","status":"SUBMITTED","subStatus":null,"txHash":null}""",
                        ),
                ),
        )

        // when / then
        mockMvc
            .perform(
                post("/api/v1/transactions")
                    .with(writeJwt())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(submitTransactionJson(externalTxId, vault.id.value)),
            ).andExpect(status().isCreated)
            .andExpect(jsonPath("$.externalTxId").value(externalTxId))
            .andExpect(jsonPath("$.fireblocksTransactionId").value("fb-created-tx-001"))
            .andExpect(jsonPath("$.status").value("SUBMITTED"))
            .andExpect(jsonPath("$.currency").value("BTC"))
            .andExpect(jsonPath("$.protocol").value("BTC"))
    }

    @Test
    fun `should reject unauthenticated request with 401`() {
        // when / then
        mockMvc
            .perform(
                post("/api/v1/transactions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(submitTransactionJson("ext-001", UUID.randomUUID())),
            ).andExpect(status().isUnauthorized)
    }

    @Test
    fun `should reject custody read scope for POST with 403`() {
        // when / then
        mockMvc
            .perform(
                post("/api/v1/transactions")
                    .with(readJwt())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(submitTransactionJson("ext-001", UUID.randomUUID())),
            ).andExpect(status().isForbidden)
    }

    @Test
    fun `should get transaction with custody read scope`() {
        // given
        val vault = vaultRepository.save(aVault(fireblocksVaultId = "fb-get-001"))
        val transaction =
            transactionRepository.save(
                aTransaction(
                    externalTxId = "ext-read-${UUID.randomUUID()}",
                    sourceVaultId = vault.id.value.toString(),
                    currency = "BTC",
                    protocol = "BTC",
                    amount = BigDecimal("2.0"),
                ),
            )

        // when / then
        mockMvc
            .perform(
                get("/api/v1/transactions/${transaction.externalTxId}")
                    .with(readJwt()),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(transaction.id.value.toString()))
            .andExpect(jsonPath("$.externalTxId").value(transaction.externalTxId))
    }

    @Test
    fun `should return 201 for duplicate externalTxId idempotently`() {
        // given
        val vault = vaultRepository.save(aVault(fireblocksVaultId = "fb-dup-001"))
        val externalTxId = "ext-dup-${UUID.randomUUID()}"
        wireMock.stubFor(
            WireMock
                .post(WireMock.urlPathEqualTo("/v1/transactions"))
                .willReturn(
                    WireMock
                        .aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody(
                            """{"id":"fb-dup-tx-001","status":"SUBMITTED","subStatus":null,"txHash":null}""",
                        ),
                ),
        )

        mockMvc
            .perform(
                post("/api/v1/transactions")
                    .with(writeJwt())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(submitTransactionJson(externalTxId, vault.id.value)),
            ).andExpect(status().isCreated)

        // when / then — second call returns existing (idempotent, still 201)
        mockMvc
            .perform(
                post("/api/v1/transactions")
                    .with(writeJwt())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(submitTransactionJson(externalTxId, vault.id.value)),
            ).andExpect(status().isCreated)
            .andExpect(jsonPath("$.externalTxId").value(externalTxId))
    }

    @Test
    fun `should return 400 for invalid request body`() {
        // when / then
        mockMvc
            .perform(
                post("/api/v1/transactions")
                    .with(writeJwt())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """{"externalTxId":"","sourceVaultId":"","destinationAddress":"","currency":"","protocol":"","amount":0}""",
                    ),
            ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("CUSTODY-0001"))
    }

    private fun submitTransactionJson(
        externalTxId: String,
        sourceVaultId: UUID,
    ) = """
        {
            "externalTxId": "$externalTxId",
            "sourceVaultId": "$sourceVaultId",
            "destinationAddress": "0xdest123",
            "currency": "BTC",
            "protocol": "BTC",
            "amount": 1.5
        }
        """.trimIndent()
}
