package com.stablecoin.custody.fireblocks.infrastructure.fireblocks.client

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import com.stablecoin.custody.fireblocks.AbstractIntegrationTest
import com.stablecoin.custody.fireblocks.infrastructure.fireblocks.client.dto.CreateTransactionRequest
import com.stablecoin.custody.fireblocks.infrastructure.fireblocks.client.dto.DestinationTransferPeerPath
import com.stablecoin.custody.fireblocks.infrastructure.fireblocks.client.dto.FireblocksEstimateFeeRequest
import com.stablecoin.custody.fireblocks.infrastructure.fireblocks.client.dto.FireblocksEstimateFeeResponse
import com.stablecoin.custody.fireblocks.infrastructure.fireblocks.client.dto.FireblocksFeeLevel
import com.stablecoin.custody.fireblocks.infrastructure.fireblocks.client.dto.FireblocksTransactionResponse
import com.stablecoin.custody.fireblocks.infrastructure.fireblocks.client.dto.OneTimeAddress
import com.stablecoin.custody.fireblocks.infrastructure.fireblocks.client.dto.TransferPeerPath
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
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.ResourceAccessException

class FireblocksTransactionClientIntegrationTest : AbstractIntegrationTest() {
    @Autowired
    private lateinit var transactionClient: FireblocksTransactionClient

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
    fun `should create transaction`() {
        // given
        wireMock.stubFor(
            post(urlPathEqualTo("/v1/transactions"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("""{"id":"tx-001","status":"SUBMITTED","subStatus":null,"txHash":null}"""),
                ),
        )

        val request =
            CreateTransactionRequest(
                externalTxId = "ext-123",
                source = TransferPeerPath(type = "VAULT_ACCOUNT", id = "0"),
                destination =
                    DestinationTransferPeerPath(
                        type = "ONE_TIME_ADDRESS",
                        oneTimeAddress = OneTimeAddress(address = "0xabc"),
                    ),
                assetId = "ETH_TEST",
                amount = "0.001",
            )

        // when
        val result = transactionClient.createTransaction(request)

        // then
        val expected = FireblocksTransactionResponse(id = "tx-001", status = "SUBMITTED")
        assertThat(result).usingRecursiveComparison().isEqualTo(expected)
    }

    @Test
    fun `should get transaction by ID`() {
        // given
        wireMock.stubFor(
            get(urlPathEqualTo("/v1/transactions/tx-002"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody(
                            """{"id":"tx-002","status":"COMPLETED","subStatus":"CONFIRMED","txHash":"0xhash123"}""",
                        ),
                ),
        )

        // when
        val result = transactionClient.getTransaction("tx-002")

        // then
        val expected =
            FireblocksTransactionResponse(
                id = "tx-002",
                status = "COMPLETED",
                subStatus = "CONFIRMED",
                txHash = "0xhash123",
            )
        assertThat(result).usingRecursiveComparison().isEqualTo(expected)
    }

    @Test
    fun `should get transaction by external ID`() {
        // given
        wireMock.stubFor(
            get(urlPathEqualTo("/v1/transactions/external_tx_id/ext-456"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("""{"id":"tx-003","status":"BROADCASTING"}"""),
                ),
        )

        // when
        val result = transactionClient.getByExternalId("ext-456")

        // then
        val expected = FireblocksTransactionResponse(id = "tx-003", status = "BROADCASTING")
        assertThat(result).usingRecursiveComparison().isEqualTo(expected)
    }

    @Test
    fun `should estimate fee`() {
        // given
        wireMock.stubFor(
            post(urlPathEqualTo("/v1/transactions/estimate_fee"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody(
                            """{
                                "low":{"networkFee":"0.0001","gasPrice":"10","feePerByte":null},
                                "medium":{"networkFee":"0.0005","gasPrice":"20","feePerByte":null},
                                "high":{"networkFee":"0.001","gasPrice":"30","feePerByte":null}
                            }""",
                        ),
                ),
        )

        val request =
            FireblocksEstimateFeeRequest(
                assetId = "ETH_TEST",
                source = TransferPeerPath(type = "VAULT_ACCOUNT", id = "0"),
                destination =
                    DestinationTransferPeerPath(
                        type = "ONE_TIME_ADDRESS",
                        oneTimeAddress = OneTimeAddress(address = "0xabc"),
                    ),
                amount = "1.0",
            )

        // when
        val result = transactionClient.estimateFee(request)

        // then
        val expected =
            FireblocksEstimateFeeResponse(
                low = FireblocksFeeLevel(networkFee = "0.0001", gasPrice = "10"),
                medium = FireblocksFeeLevel(networkFee = "0.0005", gasPrice = "20"),
                high = FireblocksFeeLevel(networkFee = "0.001", gasPrice = "30"),
            )
        assertThat(result).usingRecursiveComparison().isEqualTo(expected)
    }

    @Test
    fun `should handle Fireblocks 400 error`() {
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

        val request =
            CreateTransactionRequest(
                externalTxId = "ext-bad",
                source = TransferPeerPath(type = "VAULT_ACCOUNT", id = "0"),
                destination =
                    DestinationTransferPeerPath(
                        type = "ONE_TIME_ADDRESS",
                        oneTimeAddress = OneTimeAddress(address = "0xabc"),
                    ),
                assetId = "ETH_TEST",
                amount = "0.001",
            )

        // when/then
        assertThatThrownBy { transactionClient.createTransaction(request) }
            .isInstanceOf(HttpClientErrorException::class.java)
    }

    @Test
    fun `should handle Fireblocks 500 error`() {
        // given
        wireMock.stubFor(
            get(urlPathEqualTo("/v1/transactions/tx-error"))
                .willReturn(
                    aResponse()
                        .withStatus(500)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("""{"message":"Internal server error"}"""),
                ),
        )

        // when/then
        assertThatThrownBy { transactionClient.getTransaction("tx-error") }
            .isInstanceOf(HttpServerErrorException::class.java)
    }

    @Test
    fun `should handle timeout`() {
        // given
        wireMock.stubFor(
            get(urlPathEqualTo("/v1/transactions/tx-slow"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("""{"id":"tx-slow","status":"SUBMITTED"}""")
                        .withFixedDelay(10_000),
                ),
        )

        // when/then
        assertThatThrownBy { transactionClient.getTransaction("tx-slow") }
            .isInstanceOf(ResourceAccessException::class.java)
    }
}
