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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.UUID

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class VaultControllerIntegrationTest : AbstractIntegrationTest() {
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

    private fun writeJwt() = jwt().jwt { it.subject(UUID.randomUUID().toString()).claim("scope", "custody:write") }

    private fun readJwt() = jwt().jwt { it.subject(UUID.randomUUID().toString()).claim("scope", "custody:read") }

    @Test
    fun `should create vault via REST API end-to-end`() {
        // given
        val customerRefId = "int-test-${UUID.randomUUID()}"
        wireMock.stubFor(
            WireMock
                .post(WireMock.urlPathEqualTo("/v1/vault/accounts"))
                .willReturn(
                    WireMock
                        .aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("""{"id":"fb-created-001","name":"Test Vault","customerRefId":"$customerRefId"}"""),
                ),
        )

        // when / then
        mockMvc
            .perform(
                post("/api/v1/vaults")
                    .with(writeJwt())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"customerRefId":"$customerRefId","name":"Test Vault"}"""),
            ).andExpect(status().isCreated)
            .andExpect(jsonPath("$.customerRefId").value(customerRefId))
            .andExpect(jsonPath("$.name").value("Test Vault"))
            .andExpect(jsonPath("$.fireblocksVaultId").value("fb-created-001"))
            .andExpect(jsonPath("$.status").value("ACTIVE"))
    }

    @Test
    fun `should reject unauthenticated request with 401`() {
        mockMvc
            .perform(
                post("/api/v1/vaults")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"customerRefId":"ref-001","name":"Vault"}"""),
            ).andExpect(status().isUnauthorized)
    }

    @Test
    fun `should reject request without custody write scope with 403`() {
        mockMvc
            .perform(
                post("/api/v1/vaults")
                    .with(readJwt())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"customerRefId":"ref-001","name":"Vault"}"""),
            ).andExpect(status().isForbidden)
    }

    @Test
    fun `should accept GET with custody read scope`() {
        // given
        val vault = vaultRepository.save(aVault())

        // when / then
        mockMvc
            .perform(
                get("/api/v1/vaults/${vault.id.value}")
                    .with(readJwt()),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(vault.id.value.toString()))
    }

    @Test
    fun `should return 400 for invalid request body`() {
        mockMvc
            .perform(
                post("/api/v1/vaults")
                    .with(writeJwt())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"customerRefId":"","name":""}"""),
            ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("CUSTODY-0001"))
    }

    @Test
    fun `should return 409 for duplicate customerRefId`() {
        // given
        val customerRefId = "dup-ref-${UUID.randomUUID()}"
        wireMock.stubFor(
            WireMock
                .post(WireMock.urlPathEqualTo("/v1/vault/accounts"))
                .willReturn(
                    WireMock
                        .aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("""{"id":"fb-dup-001","name":"Vault","customerRefId":"$customerRefId"}"""),
                ),
        )
        mockMvc
            .perform(
                post("/api/v1/vaults")
                    .with(writeJwt())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"customerRefId":"$customerRefId","name":"Vault"}"""),
            ).andExpect(status().isCreated)

        // when / then — second call with same customerRefId returns existing (201 by service design)
        mockMvc
            .perform(
                post("/api/v1/vaults")
                    .with(writeJwt())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"customerRefId":"$customerRefId","name":"Vault"}"""),
            ).andExpect(status().isCreated)
            .andExpect(jsonPath("$.customerRefId").value(customerRefId))
    }
}
