package com.stablecoin.custody.fireblocks.application.controller

import com.stablecoin.custody.fireblocks.application.exception.GlobalExceptionHandler
import com.stablecoin.custody.fireblocks.domain.exception.VaultNotFoundException
import com.stablecoin.custody.fireblocks.domain.vault.VaultId
import com.stablecoin.custody.fireblocks.domain.wallet.ActivateAssetCommand
import com.stablecoin.custody.fireblocks.domain.wallet.GenerateAddressCommand
import com.stablecoin.custody.fireblocks.domain.wallet.WalletAssetService
import com.stablecoin.custody.fireblocks.test.fixtures.aDepositAddress
import com.stablecoin.custody.fireblocks.test.fixtures.aWalletAsset
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.util.UUID

class WalletAssetControllerTest {
    private val walletAssetService: WalletAssetService = mockk()
    private val controller = WalletAssetController(walletAssetService)
    private lateinit var mockMvc: MockMvc

    @BeforeEach
    fun setUp() {
        mockMvc =
            MockMvcBuilders
                .standaloneSetup(controller)
                .setMessageConverters(JacksonJsonHttpMessageConverter())
                .setControllerAdvice(GlobalExceptionHandler())
                .build()
    }

    @Test
    fun `should activate asset with currency and protocol and return 201`() {
        // given
        val vaultId = UUID.randomUUID()
        val walletAsset = aWalletAsset(vaultId = VaultId(vaultId), currency = "EURC", protocol = "ETH", fireblocksAssetId = "EURC_ETH")
        val command = ActivateAssetCommand(VaultId(vaultId), "EURC", "ETH")
        every { walletAssetService.activateAsset(command) } returns walletAsset

        // when / then
        mockMvc
            .perform(
                post("/api/v1/vaults/$vaultId/assets")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"currency":"EURC","protocol":"ETH"}"""),
            ).andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").value(walletAsset.id.value.toString()))
            .andExpect(jsonPath("$.vaultId").value(vaultId.toString()))
            .andExpect(jsonPath("$.currency").value("EURC"))
            .andExpect(jsonPath("$.protocol").value("ETH"))
            .andExpect(jsonPath("$.fireblocksAssetId").value("EURC_ETH"))
            .andExpect(jsonPath("$.status").value("ACTIVE"))
    }

    @Test
    fun `should generate deposit address and return 201`() {
        // given
        val vaultId = UUID.randomUUID()
        val address = aDepositAddress(address = "0xabc123", tag = "memo-1", legacyAddress = "legacy-addr")
        val command = GenerateAddressCommand(VaultId(vaultId), "EURC", "ETH")
        every { walletAssetService.generateDepositAddress(command) } returns address

        // when / then
        mockMvc
            .perform(post("/api/v1/vaults/$vaultId/assets/EURC/ETH/addresses"))
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").value(address.id.value.toString()))
            .andExpect(jsonPath("$.walletAssetId").value(address.walletAssetId.value.toString()))
            .andExpect(jsonPath("$.address").value("0xabc123"))
            .andExpect(jsonPath("$.tag").value("memo-1"))
            .andExpect(jsonPath("$.legacyAddress").value("legacy-addr"))
            .andExpect(jsonPath("$.createdAt").exists())
    }

    @Test
    fun `should return 404 when vault not found`() {
        // given
        val vaultId = UUID.randomUUID()
        val command = ActivateAssetCommand(VaultId(vaultId), "EURC", "ETH")
        every { walletAssetService.activateAsset(command) } throws VaultNotFoundException(vaultId.toString())

        // when / then
        mockMvc
            .perform(
                post("/api/v1/vaults/$vaultId/assets")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"currency":"EURC","protocol":"ETH"}"""),
            ).andExpect(status().isNotFound)
    }

    @Test
    fun `should return 400 for blank currency`() {
        // when / then
        mockMvc
            .perform(
                post("/api/v1/vaults/${UUID.randomUUID()}/assets")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"currency":"","protocol":"ETH"}"""),
            ).andExpect(status().isBadRequest)
    }

    @Test
    fun `should return 400 for blank protocol`() {
        // when / then
        mockMvc
            .perform(
                post("/api/v1/vaults/${UUID.randomUUID()}/assets")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"currency":"EURC","protocol":""}"""),
            ).andExpect(status().isBadRequest)
    }

    @Test
    fun `should map response with currency, protocol, and fireblocksAssetId`() {
        // given
        val vaultId = UUID.randomUUID()
        val walletAsset =
            aWalletAsset(
                vaultId = VaultId(vaultId),
                currency = "BTC",
                protocol = "BTC",
                fireblocksAssetId = "BTC",
            )
        val command = ActivateAssetCommand(VaultId(vaultId), "BTC", "BTC")
        every { walletAssetService.activateAsset(command) } returns walletAsset

        // when / then
        mockMvc
            .perform(
                post("/api/v1/vaults/$vaultId/assets")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"currency":"BTC","protocol":"BTC"}"""),
            ).andExpect(status().isCreated)
            .andExpect(jsonPath("$.currency").value("BTC"))
            .andExpect(jsonPath("$.protocol").value("BTC"))
            .andExpect(jsonPath("$.fireblocksAssetId").value("BTC"))
            .andExpect(jsonPath("$.createdAt").exists())
            .andExpect(jsonPath("$.updatedAt").exists())
    }
}
