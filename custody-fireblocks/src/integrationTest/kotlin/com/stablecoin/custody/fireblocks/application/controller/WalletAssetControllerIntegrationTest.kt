package com.stablecoin.custody.fireblocks.application.controller

import com.github.tomakehurst.wiremock.client.WireMock
import com.stablecoin.custody.fireblocks.AbstractMockMvcIntegrationTest
import com.stablecoin.custody.fireblocks.domain.vault.VaultRepository
import com.stablecoin.custody.fireblocks.domain.wallet.WalletAssetService
import com.stablecoin.custody.fireblocks.test.fixtures.aVault
import com.stablecoin.custody.fireblocks.test.fixtures.anActivateAssetCommand
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.UUID

class WalletAssetControllerIntegrationTest : AbstractMockMvcIntegrationTest() {
    @Autowired
    private lateinit var vaultRepository: VaultRepository

    @Autowired
    private lateinit var walletAssetService: WalletAssetService

    @BeforeEach
    fun resetWireMock() {
        wireMock.resetAll()
    }

    private fun writeJwt() = jwt().jwt { it.subject(UUID.randomUUID().toString()).claim("scope", "custody:write") }

    @Test
    fun `should activate asset with currency and protocol end-to-end`() {
        // given
        val vault = vaultRepository.save(aVault(fireblocksVaultId = "fb-ctrl-001"))
        wireMock.stubFor(
            WireMock
                .post(WireMock.urlPathEqualTo("/v1/vault/accounts/fb-ctrl-001/EURC"))
                .willReturn(
                    WireMock
                        .aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("""{"id":"EURC","available":"0"}"""),
                ),
        )

        // when / then
        mockMvc
            .perform(
                post("/api/v1/vaults/${vault.id.value}/assets")
                    .with(writeJwt())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"currency":"EURC","protocol":"ETH"}"""),
            ).andExpect(status().isCreated)
            .andExpect(jsonPath("$.vaultId").value(vault.id.value.toString()))
            .andExpect(jsonPath("$.currency").value("EURC"))
            .andExpect(jsonPath("$.protocol").value("ETH"))
            .andExpect(jsonPath("$.fireblocksAssetId").value("EURC"))
            .andExpect(jsonPath("$.status").value("ACTIVE"))
    }

    @Test
    fun `should generate address using currency and protocol path end-to-end`() {
        // given
        val vault = vaultRepository.save(aVault(fireblocksVaultId = "fb-ctrl-002"))
        wireMock.stubFor(
            WireMock
                .post(WireMock.urlPathEqualTo("/v1/vault/accounts/fb-ctrl-002/ETH"))
                .willReturn(
                    WireMock
                        .aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("""{"id":"ETH","available":"0"}"""),
                ),
        )
        walletAssetService.activateAsset(anActivateAssetCommand(vaultId = vault.id, currency = "ETH", protocol = "ETH"))

        wireMock.stubFor(
            WireMock
                .post(WireMock.urlPathEqualTo("/v1/vault/accounts/fb-ctrl-002/ETH/addresses"))
                .willReturn(
                    WireMock
                        .aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("""{"address":"0xdeadbeef1234567890","tag":null,"legacyAddress":null}"""),
                ),
        )

        // when / then
        mockMvc
            .perform(
                post("/api/v1/vaults/${vault.id.value}/assets/ETH/ETH/addresses")
                    .with(writeJwt()),
            ).andExpect(status().isCreated)
            .andExpect(jsonPath("$.address").value("0xdeadbeef1234567890"))
            .andExpect(jsonPath("$.walletAssetId").exists())
            .andExpect(jsonPath("$.createdAt").exists())
    }

    @Test
    fun `should return 404 for unsupported currency-protocol pair`() {
        // given
        val vault = vaultRepository.save(aVault(fireblocksVaultId = "fb-ctrl-003"))

        // when / then
        mockMvc
            .perform(
                post("/api/v1/vaults/${vault.id.value}/assets")
                    .with(writeJwt())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"currency":"FAKE","protocol":"FAKE"}"""),
            ).andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("CUSTODY-1003"))
    }
}
