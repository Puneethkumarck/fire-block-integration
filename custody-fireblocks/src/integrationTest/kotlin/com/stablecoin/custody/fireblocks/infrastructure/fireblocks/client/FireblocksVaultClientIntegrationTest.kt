package com.stablecoin.custody.fireblocks.infrastructure.fireblocks.client

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.matching
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import com.stablecoin.custody.fireblocks.AbstractIntegrationTest
import com.stablecoin.custody.fireblocks.infrastructure.fireblocks.client.dto.CreateVaultAccountRequest
import com.stablecoin.custody.fireblocks.infrastructure.fireblocks.client.dto.FireblocksBalanceResponse
import com.stablecoin.custody.fireblocks.infrastructure.fireblocks.client.dto.FireblocksDepositAddressResponse
import com.stablecoin.custody.fireblocks.infrastructure.fireblocks.client.dto.FireblocksVaultAccountResponse
import com.stablecoin.custody.fireblocks.infrastructure.fireblocks.client.dto.FireblocksWalletAssetResponse
import com.stablecoin.custody.fireblocks.infrastructure.fireblocks.client.dto.GenerateAddressRequest
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

class FireblocksVaultClientIntegrationTest : AbstractIntegrationTest() {
    @Autowired
    private lateinit var vaultClient: FireblocksVaultClient

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
    fun `should create vault account`() {
        // given
        wireMock.stubFor(
            post(urlPathEqualTo("/v1/vault/accounts"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody(
                            """{"id":"123","name":"Test Vault","customerRefId":"cust-001"}""",
                        ),
                ),
        )

        // when
        val result =
            vaultClient.createVault(
                CreateVaultAccountRequest(name = "Test Vault", customerRefId = "cust-001"),
            )

        // then
        val expected = FireblocksVaultAccountResponse(id = "123", name = "Test Vault", customerRefId = "cust-001")
        assertThat(result).usingRecursiveComparison().isEqualTo(expected)
    }

    @Test
    fun `should get vault account`() {
        // given
        wireMock.stubFor(
            get(urlPathEqualTo("/v1/vault/accounts/456"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody(
                            """{"id":"456","name":"My Vault","customerRefId":"cust-002"}""",
                        ),
                ),
        )

        // when
        val result = vaultClient.getVault("456")

        // then
        val expected = FireblocksVaultAccountResponse(id = "456", name = "My Vault", customerRefId = "cust-002")
        assertThat(result).usingRecursiveComparison().isEqualTo(expected)
    }

    @Test
    fun `should create wallet asset`() {
        // given
        wireMock.stubFor(
            post(urlPathEqualTo("/v1/vault/accounts/123/ETH_TEST"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("""{"id":"ETH_TEST","available":"0"}"""),
                ),
        )

        // when
        val result = vaultClient.createWalletAsset("123", "ETH_TEST")

        // then
        val expected = FireblocksWalletAssetResponse(id = "ETH_TEST", available = "0")
        assertThat(result).usingRecursiveComparison().isEqualTo(expected)
    }

    @Test
    fun `should generate deposit address`() {
        // given
        wireMock.stubFor(
            post(urlPathEqualTo("/v1/vault/accounts/123/ETH_TEST/addresses"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("""{"address":"0xabc123","tag":null,"legacyAddress":null}"""),
                ),
        )

        // when
        val result =
            vaultClient.generateDepositAddress(
                "123",
                "ETH_TEST",
                GenerateAddressRequest(description = "test"),
            )

        // then
        val expected = FireblocksDepositAddressResponse(address = "0xabc123")
        assertThat(result).usingRecursiveComparison().isEqualTo(expected)
    }

    @Test
    fun `should refresh balance`() {
        // given
        wireMock.stubFor(
            post(urlPathEqualTo("/v1/vault/accounts/123/ETH_TEST/balance"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody(
                            """{"id":"ETH_TEST","total":"10.5","available":"8.0","pending":"2.5","frozen":"0","locked":"0"}""",
                        ),
                ),
        )

        // when
        val result = vaultClient.refreshBalance("123", "ETH_TEST")

        // then
        val expected =
            FireblocksBalanceResponse(
                id = "ETH_TEST",
                total = "10.5",
                available = "8.0",
                pending = "2.5",
                frozen = "0",
                locked = "0",
            )
        assertThat(result).usingRecursiveComparison().isEqualTo(expected)
    }

    @Test
    fun `should handle Fireblocks 400 error`() {
        // given
        wireMock.stubFor(
            post(urlPathEqualTo("/v1/vault/accounts"))
                .willReturn(
                    aResponse()
                        .withStatus(400)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("""{"message":"Invalid request","code":1000}"""),
                ),
        )

        // when/then
        assertThatThrownBy {
            vaultClient.createVault(
                CreateVaultAccountRequest(name = "Bad", customerRefId = "bad"),
            )
        }.isInstanceOf(HttpClientErrorException::class.java)
    }

    @Test
    fun `should handle Fireblocks 500 error`() {
        // given
        wireMock.stubFor(
            get(urlPathEqualTo("/v1/vault/accounts/999"))
                .willReturn(
                    aResponse()
                        .withStatus(500)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("""{"message":"Internal server error"}"""),
                ),
        )

        // when/then
        assertThatThrownBy { vaultClient.getVault("999") }
            .isInstanceOf(HttpServerErrorException::class.java)
    }

    @Test
    fun `should send Authorization and X-API-Key headers`() {
        // given
        wireMock.stubFor(
            get(urlPathEqualTo("/v1/vault/accounts/789"))
                .withHeader("Authorization", matching("Bearer .+"))
                .withHeader("X-API-Key", equalTo("test-api-key"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("""{"id":"789","name":"Auth Test"}"""),
                ),
        )

        // when
        val result = vaultClient.getVault("789")

        // then
        val expected = FireblocksVaultAccountResponse(id = "789", name = "Auth Test")
        assertThat(result).usingRecursiveComparison().isEqualTo(expected)
    }
}
