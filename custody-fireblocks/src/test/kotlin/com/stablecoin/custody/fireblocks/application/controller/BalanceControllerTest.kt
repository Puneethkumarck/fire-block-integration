package com.stablecoin.custody.fireblocks.application.controller

import com.stablecoin.custody.fireblocks.application.exception.GlobalExceptionHandler
import com.stablecoin.custody.fireblocks.domain.exception.VaultNotFoundException
import com.stablecoin.custody.fireblocks.domain.port.BalanceResult
import com.stablecoin.custody.fireblocks.domain.port.FireblocksBalancePort
import com.stablecoin.custody.fireblocks.domain.vault.VaultId
import com.stablecoin.custody.fireblocks.domain.vault.VaultQueryService
import com.stablecoin.custody.fireblocks.domain.wallet.SupportedAssetRepository
import com.stablecoin.custody.fireblocks.test.fixtures.aSupportedAsset
import com.stablecoin.custody.fireblocks.test.fixtures.aVault
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.math.BigDecimal
import java.util.UUID

class BalanceControllerTest {
    private val vaultQueryService: VaultQueryService = mockk()
    private val supportedAssetRepository: SupportedAssetRepository = mockk()
    private val fireblocksBalancePort: FireblocksBalancePort = mockk()
    private val controller = BalanceController(vaultQueryService, supportedAssetRepository, fireblocksBalancePort)
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
    fun `should return balance for vault asset using currency and protocol path params`() {
        // given
        val vaultId = UUID.randomUUID()
        val vault = aVault(id = VaultId(vaultId), fireblocksVaultId = "fb-vault-1")
        val supportedAsset = aSupportedAsset(currency = "EURC", protocol = "ETH", fireblocksAssetId = "EURC_ETH")
        val balance =
            BalanceResult(
                total = BigDecimal("1000.00"),
                available = BigDecimal("800.00"),
                pending = BigDecimal("100.00"),
                frozen = BigDecimal("50.00"),
                locked = BigDecimal("50.00"),
            )
        every { vaultQueryService.getVault(VaultId(vaultId)) } returns vault
        every { supportedAssetRepository.findByCurrencyAndProtocol("EURC", "ETH") } returns supportedAsset
        every { fireblocksBalancePort.getBalance("fb-vault-1", "EURC_ETH", false) } returns balance

        // when / then
        mockMvc
            .perform(get("/api/v1/vaults/$vaultId/assets/EURC/ETH/balance"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.total").value(1000.00))
            .andExpect(jsonPath("$.available").value(800.00))
            .andExpect(jsonPath("$.pending").value(100.00))
            .andExpect(jsonPath("$.frozen").value(50.00))
            .andExpect(jsonPath("$.locked").value(50.00))
    }

    @Test
    fun `should resolve currency and protocol to fireblocksAssetId for balance query`() {
        // given
        val vaultId = UUID.randomUUID()
        val vault = aVault(id = VaultId(vaultId), fireblocksVaultId = "fb-vault-2")
        val supportedAsset = aSupportedAsset(currency = "BTC", protocol = "BTC", fireblocksAssetId = "BTC")
        val balance =
            BalanceResult(
                total = BigDecimal("5.00"),
                available = BigDecimal("4.00"),
                pending = BigDecimal("1.00"),
                frozen = BigDecimal.ZERO,
                locked = BigDecimal.ZERO,
            )
        every { vaultQueryService.getVault(VaultId(vaultId)) } returns vault
        every { supportedAssetRepository.findByCurrencyAndProtocol("BTC", "BTC") } returns supportedAsset
        every { fireblocksBalancePort.getBalance("fb-vault-2", "BTC", false) } returns balance

        // when / then
        mockMvc
            .perform(get("/api/v1/vaults/$vaultId/assets/BTC/BTC/balance"))
            .andExpect(status().isOk)

        verify { fireblocksBalancePort.getBalance("fb-vault-2", "BTC", false) }
    }

    @Test
    fun `should pass refresh parameter to port`() {
        // given
        val vaultId = UUID.randomUUID()
        val vault = aVault(id = VaultId(vaultId), fireblocksVaultId = "fb-vault-3")
        val supportedAsset = aSupportedAsset(currency = "EURC", protocol = "ETH", fireblocksAssetId = "EURC_ETH")
        val balance =
            BalanceResult(
                total = BigDecimal("500.00"),
                available = BigDecimal("500.00"),
                pending = BigDecimal.ZERO,
                frozen = BigDecimal.ZERO,
                locked = BigDecimal.ZERO,
            )
        every { vaultQueryService.getVault(VaultId(vaultId)) } returns vault
        every { supportedAssetRepository.findByCurrencyAndProtocol("EURC", "ETH") } returns supportedAsset
        every { fireblocksBalancePort.getBalance("fb-vault-3", "EURC_ETH", true) } returns balance

        // when / then
        mockMvc
            .perform(get("/api/v1/vaults/$vaultId/assets/EURC/ETH/balance?refresh=true"))
            .andExpect(status().isOk)

        verify { fireblocksBalancePort.getBalance("fb-vault-3", "EURC_ETH", true) }
    }

    @Test
    fun `should look up vault to resolve fireblocksVaultId`() {
        // given
        val vaultId = UUID.randomUUID()
        val vault = aVault(id = VaultId(vaultId), fireblocksVaultId = "fb-resolved-id")
        val supportedAsset = aSupportedAsset(currency = "EURC", protocol = "ETH", fireblocksAssetId = "EURC_ETH")
        val balance =
            BalanceResult(
                total = BigDecimal("100.00"),
                available = BigDecimal("100.00"),
                pending = BigDecimal.ZERO,
                frozen = BigDecimal.ZERO,
                locked = BigDecimal.ZERO,
            )
        every { vaultQueryService.getVault(VaultId(vaultId)) } returns vault
        every { supportedAssetRepository.findByCurrencyAndProtocol("EURC", "ETH") } returns supportedAsset
        every { fireblocksBalancePort.getBalance("fb-resolved-id", "EURC_ETH", false) } returns balance

        // when / then
        mockMvc
            .perform(get("/api/v1/vaults/$vaultId/assets/EURC/ETH/balance"))
            .andExpect(status().isOk)

        verify { vaultQueryService.getVault(VaultId(vaultId)) }
        verify { fireblocksBalancePort.getBalance("fb-resolved-id", "EURC_ETH", false) }
    }

    @Test
    fun `should return 404 when vault not found`() {
        // given
        val vaultId = UUID.randomUUID()
        every { vaultQueryService.getVault(VaultId(vaultId)) } throws VaultNotFoundException(vaultId.toString())

        // when / then
        mockMvc
            .perform(get("/api/v1/vaults/$vaultId/assets/EURC/ETH/balance"))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `should return 404 for unsupported currency-protocol pair`() {
        // given
        val vaultId = UUID.randomUUID()
        val vault = aVault(id = VaultId(vaultId), fireblocksVaultId = "fb-vault-4")
        every { vaultQueryService.getVault(VaultId(vaultId)) } returns vault
        every { supportedAssetRepository.findByCurrencyAndProtocol("FAKE", "FAKE") } returns null

        // when / then
        mockMvc
            .perform(get("/api/v1/vaults/$vaultId/assets/FAKE/FAKE/balance"))
            .andExpect(status().isNotFound)
    }
}
