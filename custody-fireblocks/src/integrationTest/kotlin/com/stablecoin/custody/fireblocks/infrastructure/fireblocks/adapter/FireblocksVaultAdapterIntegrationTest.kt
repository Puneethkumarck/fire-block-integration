package com.stablecoin.custody.fireblocks.infrastructure.fireblocks.adapter

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import com.stablecoin.custody.fireblocks.AbstractIntegrationTest
import com.stablecoin.custody.fireblocks.domain.port.FireblocksVaultPort
import com.stablecoin.custody.fireblocks.domain.port.VaultResult
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
import org.springframework.web.client.HttpServerErrorException

class FireblocksVaultAdapterIntegrationTest : AbstractIntegrationTest() {
    @Autowired
    private lateinit var fireblocksVaultPort: FireblocksVaultPort

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
    fun `should create vault through full adapter chain`() {
        // given
        wireMock.stubFor(
            post(urlPathEqualTo("/v1/vault/accounts"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("""{"id":"fb-123","name":"Integration Vault","customerRefId":"cust-int-001"}"""),
                ),
        )

        // when
        val result = fireblocksVaultPort.createVault("Integration Vault", "cust-int-001")

        // then
        val expected = VaultResult(id = "fb-123", name = "Integration Vault")
        assertThat(result).usingRecursiveComparison().isEqualTo(expected)
    }

    @Test
    fun `should handle Fireblocks error in adapter`() {
        // given
        wireMock.stubFor(
            post(urlPathEqualTo("/v1/vault/accounts"))
                .willReturn(
                    aResponse()
                        .withStatus(500)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("""{"message":"Internal server error"}"""),
                ),
        )

        // when
        // then
        assertThatThrownBy { fireblocksVaultPort.createVault("Fail Vault", "cust-fail") }
            .isInstanceOf(HttpServerErrorException::class.java)
    }
}
