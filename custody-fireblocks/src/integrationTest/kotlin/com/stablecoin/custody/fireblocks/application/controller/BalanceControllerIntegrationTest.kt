package com.stablecoin.custody.fireblocks.application.controller

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import com.stablecoin.custody.fireblocks.AbstractIntegrationTest
import com.stablecoin.custody.fireblocks.domain.vault.VaultRepository
import com.stablecoin.custody.fireblocks.test.fixtures.aVault
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.UUID

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class BalanceControllerIntegrationTest : AbstractIntegrationTest() {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var vaultRepository: VaultRepository

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

    private fun readJwt() = jwt().jwt { it.subject(UUID.randomUUID().toString()).claim("scope", "custody:read") }

    @Test
    fun `should return balance from Fireblocks using currency and protocol`() {
        // given
        val vault = vaultRepository.save(aVault(fireblocksVaultId = "fb-bal-001"))
        wireMock.stubFor(
            WireMock
                .post(WireMock.urlPathEqualTo("/v1/vault/accounts/fb-bal-001/EURC/balance"))
                .willReturn(
                    WireMock
                        .aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody(
                            """{"id":"EURC","total":"1000.50","available":"800.25","pending":"100.25","frozen":"50.00","locked":"50.00"}""",
                        ),
                ),
        )

        // when / then
        mockMvc
            .perform(
                get("/api/v1/vaults/${vault.id.value}/assets/EURC/ETH/balance")
                    .with(readJwt()),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.total").value(1000.50))
            .andExpect(jsonPath("$.available").value(800.25))
            .andExpect(jsonPath("$.pending").value(100.25))
            .andExpect(jsonPath("$.frozen").value(50.00))
            .andExpect(jsonPath("$.locked").value(50.00))
    }

    @Test
    fun `should pass refresh=true to Fireblocks`() {
        // given
        val vault = vaultRepository.save(aVault(fireblocksVaultId = "fb-bal-002"))
        wireMock.stubFor(
            WireMock
                .post(WireMock.urlPathEqualTo("/v1/vault/accounts/fb-bal-002/EURC/balance"))
                .willReturn(
                    WireMock
                        .aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody(
                            """{"id":"EURC","total":"500.00","available":"500.00","pending":"0","frozen":"0","locked":"0"}""",
                        ),
                ),
        )

        // when / then
        mockMvc
            .perform(
                get("/api/v1/vaults/${vault.id.value}/assets/EURC/ETH/balance?refresh=true")
                    .with(readJwt()),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.total").value(500.00))
    }

    @Test
    fun `should return 404 for unsupported currency-protocol pair`() {
        // given
        val vault = vaultRepository.save(aVault(fireblocksVaultId = "fb-bal-003"))

        // when / then
        mockMvc
            .perform(
                get("/api/v1/vaults/${vault.id.value}/assets/FAKE/FAKE/balance")
                    .with(readJwt()),
            ).andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("CUSTODY-1003"))
    }
}
