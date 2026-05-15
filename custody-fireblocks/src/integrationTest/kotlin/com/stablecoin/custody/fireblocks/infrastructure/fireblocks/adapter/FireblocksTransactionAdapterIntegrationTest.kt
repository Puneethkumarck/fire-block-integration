package com.stablecoin.custody.fireblocks.infrastructure.fireblocks.adapter

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import com.stablecoin.custody.fireblocks.AbstractIntegrationTest
import com.stablecoin.custody.fireblocks.domain.port.FireblocksSubmitCommand
import com.stablecoin.custody.fireblocks.domain.port.FireblocksTransactionPort
import com.stablecoin.custody.fireblocks.domain.port.TransactionResult
import com.stablecoin.custody.fireblocks.domain.transaction.FeeLevel
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.web.client.HttpClientErrorException
import java.math.BigDecimal

class FireblocksTransactionAdapterIntegrationTest : AbstractIntegrationTest() {
    @Autowired
    private lateinit var fireblocksTransactionPort: FireblocksTransactionPort

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

    @Test
    fun `should submit transaction through full adapter chain`() {
        // given
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
            FireblocksSubmitCommand(
                externalTxId = "ext-int-001",
                sourceVaultId = "vault-1",
                destinationAddress = "0xintegration",
                assetId = "ETH_TEST",
                amount = BigDecimal("0.5"),
                feeLevel = FeeLevel.MEDIUM,
                treatAsGrossAmount = false,
                note = "Integration test",
            )

        // when
        val result = fireblocksTransactionPort.submitTransaction(command)

        // then
        val expected =
            TransactionResult(
                id = "fb-tx-int-001",
                status = "SUBMITTED",
                subStatus = null,
                txHash = null,
            )
        assertThat(result).usingRecursiveComparison().isEqualTo(expected)
    }

    @Test
    fun `should propagate Fireblocks error as exception`() {
        // given
        wireMock.stubFor(
            post(urlPathEqualTo("/v1/transactions"))
                .willReturn(
                    aResponse()
                        .withStatus(400)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("""{"message":"Invalid request","code":1000}"""),
                ),
        )
        val command =
            FireblocksSubmitCommand(
                externalTxId = "ext-fail",
                sourceVaultId = "vault-1",
                destinationAddress = "0xfail",
                assetId = "ETH_TEST",
                amount = BigDecimal("0.1"),
                feeLevel = FeeLevel.LOW,
                treatAsGrossAmount = false,
                note = null,
            )

        // when
        // then
        assertThatThrownBy { fireblocksTransactionPort.submitTransaction(command) }
            .isInstanceOf(HttpClientErrorException::class.java)
    }
}
