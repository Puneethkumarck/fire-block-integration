package com.stablecoin.custody.fireblocks.infrastructure.fireblocks.adapter

import com.stablecoin.custody.fireblocks.domain.port.BalanceResult
import com.stablecoin.custody.fireblocks.infrastructure.fireblocks.client.FireblocksVaultClient
import com.stablecoin.custody.fireblocks.infrastructure.fireblocks.client.dto.FireblocksBalanceResponse
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class FireblocksBalanceAdapterTest {
    private val vaultClient = mockk<FireblocksVaultClient>()
    private val adapter = FireblocksBalanceAdapter(vaultClient)

    @Test
    fun `should get balance and map to BigDecimal values`() {
        // given
        val response =
            FireblocksBalanceResponse(
                id = "ETH_TEST",
                total = "10.5",
                available = "8.0",
                pending = "2.5",
                frozen = "0.0",
                locked = "0.0",
            )
        every { vaultClient.refreshBalance("vault-1", "ETH_TEST") } returns response

        // when
        val result = adapter.getBalance("vault-1", "ETH_TEST", refresh = true)

        // then
        val expected =
            BalanceResult(
                total = BigDecimal("10.5"),
                available = BigDecimal("8.0"),
                pending = BigDecimal("2.5"),
                frozen = BigDecimal("0.0"),
                locked = BigDecimal("0.0"),
            )
        assertThat(result).usingRecursiveComparison().isEqualTo(expected)
    }

    @Test
    fun `should handle zero balances`() {
        // given
        val response =
            FireblocksBalanceResponse(
                id = "ETH_TEST",
                total = "0",
                available = "0",
                pending = "0",
                frozen = "0",
                locked = "0",
            )
        every { vaultClient.refreshBalance("vault-2", "ETH_TEST") } returns response

        // when
        val result = adapter.getBalance("vault-2", "ETH_TEST", refresh = false)

        // then
        val expected =
            BalanceResult(
                total = BigDecimal("0"),
                available = BigDecimal("0"),
                pending = BigDecimal("0"),
                frozen = BigDecimal("0"),
                locked = BigDecimal("0"),
            )
        assertThat(result).usingRecursiveComparison().isEqualTo(expected)
    }
}
