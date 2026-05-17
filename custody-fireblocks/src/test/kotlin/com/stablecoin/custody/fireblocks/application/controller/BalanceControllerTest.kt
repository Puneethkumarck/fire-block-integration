package com.stablecoin.custody.fireblocks.application.controller

import com.stablecoin.custody.fireblocks.application.exception.GlobalExceptionHandler
import com.stablecoin.custody.fireblocks.domain.exception.AssetNotFoundException
import com.stablecoin.custody.fireblocks.domain.exception.VaultNotFoundException
import com.stablecoin.custody.fireblocks.domain.port.BalanceResult
import com.stablecoin.custody.fireblocks.domain.vault.VaultId
import com.stablecoin.custody.fireblocks.domain.wallet.BalanceQueryService
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
    private val balanceQueryService: BalanceQueryService = mockk()
    private val controller = BalanceController(balanceQueryService)
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
        val balance =
            BalanceResult(
                total = BigDecimal("1000.00"),
                available = BigDecimal("800.00"),
                pending = BigDecimal("100.00"),
                frozen = BigDecimal("50.00"),
                locked = BigDecimal("50.00"),
            )
        every { balanceQueryService.getBalance(VaultId(vaultId), "EURC", "ETH", false) } returns balance

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
    fun `should pass refresh parameter to service`() {
        // given
        val vaultId = UUID.randomUUID()
        val balance =
            BalanceResult(
                total = BigDecimal("500.00"),
                available = BigDecimal("500.00"),
                pending = BigDecimal.ZERO,
                frozen = BigDecimal.ZERO,
                locked = BigDecimal.ZERO,
            )
        every { balanceQueryService.getBalance(VaultId(vaultId), "EURC", "ETH", true) } returns balance

        // when / then
        mockMvc
            .perform(get("/api/v1/vaults/$vaultId/assets/EURC/ETH/balance?refresh=true"))
            .andExpect(status().isOk)

        verify { balanceQueryService.getBalance(VaultId(vaultId), "EURC", "ETH", true) }
    }

    @Test
    fun `should return 404 when vault not found`() {
        // given
        val vaultId = UUID.randomUUID()
        every {
            balanceQueryService.getBalance(VaultId(vaultId), "EURC", "ETH", false)
        } throws VaultNotFoundException(vaultId.toString())

        // when / then
        mockMvc
            .perform(get("/api/v1/vaults/$vaultId/assets/EURC/ETH/balance"))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `should return 404 for unsupported currency-protocol pair`() {
        // given
        val vaultId = UUID.randomUUID()
        every {
            balanceQueryService.getBalance(VaultId(vaultId), "FAKE", "FAKE", false)
        } throws AssetNotFoundException(vaultId.toString(), "FAKE/FAKE")

        // when / then
        mockMvc
            .perform(get("/api/v1/vaults/$vaultId/assets/FAKE/FAKE/balance"))
            .andExpect(status().isNotFound)
    }
}
