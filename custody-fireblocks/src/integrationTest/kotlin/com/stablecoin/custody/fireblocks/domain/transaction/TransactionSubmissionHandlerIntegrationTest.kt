package com.stablecoin.custody.fireblocks.domain.transaction

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import com.stablecoin.custody.fireblocks.AbstractIntegrationTest
import com.stablecoin.custody.fireblocks.domain.audit.AuditLogRepository
import com.stablecoin.custody.fireblocks.domain.audit.AuditOperation
import com.stablecoin.custody.fireblocks.domain.audit.AuditStatus
import com.stablecoin.custody.fireblocks.domain.vault.Vault
import com.stablecoin.custody.fireblocks.domain.vault.VaultRepository
import com.stablecoin.custody.fireblocks.domain.vault.VaultStatus
import com.stablecoin.custody.fireblocks.test.fixtures.aSubmitTransactionCommand
import com.stablecoin.custody.fireblocks.test.fixtures.aVault
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource

class TransactionSubmissionHandlerIntegrationTest : AbstractIntegrationTest() {
    @Autowired
    private lateinit var handler: TransactionSubmissionHandler

    @Autowired
    private lateinit var transactionRepository: TransactionRepository

    @Autowired
    private lateinit var vaultRepository: VaultRepository

    @Autowired
    private lateinit var auditLogRepository: AuditLogRepository

    companion object {
        private val wireMock = WireMockServer(wireMockConfig().dynamicPort())

        @JvmStatic
        @BeforeAll
        fun startWireMock() {
            wireMock.start()
        }

        @JvmStatic
        @AfterAll
        fun stopWireMock() {
            wireMock.stop()
        }

        @JvmStatic
        @DynamicPropertySource
        fun configureWireMock(registry: DynamicPropertyRegistry) {
            registry.add("fireblocks.api.base-url") { "http://localhost:${wireMock.port()}" }
        }
    }

    @BeforeEach
    fun resetWireMock() {
        wireMock.resetAll()
    }

    private fun createActiveVault(): Vault {
        val vault = aVault(status = VaultStatus.ACTIVE, fireblocksVaultId = "fb-vault-int-001")
        return vaultRepository.save(vault)
    }

    @Test
    fun `should submit transaction end-to-end with Fireblocks`() {
        // given
        val vault = createActiveVault()
        wireMock.stubFor(
            post(urlPathMatching("/v1/vault/accounts/.+/lock_allocation"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("""{"id":"lock-int-001","status":"LOCKED"}"""),
                ),
        )
        wireMock.stubFor(
            post(urlPathEqualTo("/v1/transactions"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("""{"id":"fb-tx-int-001","status":"SUBMITTED","subStatus":null,"txHash":null}"""),
                ),
        )
        val command =
            aSubmitTransactionCommand(
                sourceVaultId = vault.id.value.toString(),
                currency = "BTC",
                protocol = "BTC",
            )

        // when
        val result = handler.handle(command)

        // then
        assertThat(result.status).isEqualTo(TransactionStatus.SUBMITTED)
        assertThat(result.fireblocksTransactionId).isEqualTo("fb-tx-int-001")
        wireMock.verify(1, postRequestedFor(urlPathMatching("/v1/vault/accounts/.+/lock_allocation")))
        wireMock.verify(1, postRequestedFor(urlPathEqualTo("/v1/transactions")))
    }

    @Test
    fun `should handle idempotent submission`() {
        // given
        val vault = createActiveVault()
        wireMock.stubFor(
            post(urlPathMatching("/v1/vault/accounts/.+/lock_allocation"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("""{"id":"lock-int-002","status":"LOCKED"}"""),
                ),
        )
        wireMock.stubFor(
            post(urlPathEqualTo("/v1/transactions"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("""{"id":"fb-tx-int-002","status":"SUBMITTED","subStatus":null,"txHash":null}"""),
                ),
        )
        val command =
            aSubmitTransactionCommand(
                sourceVaultId = vault.id.value.toString(),
                currency = "BTC",
                protocol = "BTC",
            )

        // when
        val first = handler.handle(command)
        val second = handler.handle(command)

        // then
        assertThat(first.id).isEqualTo(second.id)
        assertThat(first.externalTxId).isEqualTo(second.externalTxId)
        wireMock.verify(1, postRequestedFor(urlPathMatching("/v1/vault/accounts/.+/lock_allocation")))
        wireMock.verify(1, postRequestedFor(urlPathEqualTo("/v1/transactions")))
    }

    @Test
    fun `should persist audit log`() {
        // given
        val vault = createActiveVault()
        wireMock.stubFor(
            post(urlPathMatching("/v1/vault/accounts/.+/lock_allocation"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("""{"id":"lock-int-003","status":"LOCKED"}"""),
                ),
        )
        wireMock.stubFor(
            post(urlPathEqualTo("/v1/transactions"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("""{"id":"fb-tx-int-003","status":"SUBMITTED","subStatus":null,"txHash":null}"""),
                ),
        )
        val command =
            aSubmitTransactionCommand(
                sourceVaultId = vault.id.value.toString(),
                currency = "ETH",
                protocol = "ETH",
            )

        // when
        val result = handler.handle(command)

        // then
        val auditLogs = auditLogRepository.findByResourceId(result.id.value.toString())
        assertThat(auditLogs).anyMatch {
            it.operation == AuditOperation.TRANSACTION_SUBMITTED && it.status == AuditStatus.SUCCESS
        }
    }

    @Test
    fun `should handle Fireblocks error and mark FAILED`() {
        // given
        val vault = createActiveVault()
        wireMock.stubFor(
            post(urlPathMatching("/v1/vault/accounts/.+/lock_allocation"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("""{"id":"lock-int-004","status":"LOCKED"}"""),
                ),
        )
        wireMock.stubFor(
            post(urlPathMatching("/v1/vault/accounts/.+/release_allocation"))
                .willReturn(
                    aResponse()
                        .withStatus(200),
                ),
        )
        wireMock.stubFor(
            post(urlPathEqualTo("/v1/transactions"))
                .willReturn(
                    aResponse()
                        .withStatus(500)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("""{"message":"Internal server error","code":9000}"""),
                ),
        )
        val command =
            aSubmitTransactionCommand(
                sourceVaultId = vault.id.value.toString(),
                currency = "BTC",
                protocol = "BTC",
            )

        // when
        val result = handler.handle(command)

        // then
        assertThat(result.status).isEqualTo(TransactionStatus.FAILED)

        val persisted = transactionRepository.findByExternalTxId(command.externalTxId)
        assertThat(persisted).isNotNull
        assertThat(persisted!!.status).isEqualTo(TransactionStatus.FAILED)
        wireMock.verify(1, postRequestedFor(urlPathMatching("/v1/vault/accounts/.+/release_allocation")))
    }
}
