package com.stablecoin.custody.fireblocks.domain.wallet

import com.stablecoin.custody.fireblocks.test.fixtures.aDepositAddress
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.util.UUID

class DepositAddressTest {
    @Test
    fun `should create deposit address with tag`() {
        // given
        val walletAssetId = WalletAssetId(UUID.randomUUID())

        // when
        val result =
            DepositAddress.create(
                walletAssetId = walletAssetId,
                address = "0xabc123",
                tag = "memo-123",
                legacyAddress = "legacy-addr",
            )

        // then
        val expected =
            aDepositAddress(
                walletAssetId = walletAssetId,
                address = "0xabc123",
                tag = "memo-123",
                legacyAddress = "legacy-addr",
            )
        assertThat(result)
            .usingRecursiveComparison()
            .ignoringFields("id", "createdAt", "updatedAt")
            .isEqualTo(expected)
    }

    @Test
    fun `should create deposit address without tag`() {
        // given
        val walletAssetId = WalletAssetId(UUID.randomUUID())

        // when
        val result =
            DepositAddress.create(
                walletAssetId = walletAssetId,
                address = "0xabc123",
            )

        // then
        val expected =
            aDepositAddress(
                walletAssetId = walletAssetId,
                address = "0xabc123",
                tag = null,
                legacyAddress = null,
            )
        assertThat(result)
            .usingRecursiveComparison()
            .ignoringFields("id", "createdAt", "updatedAt")
            .isEqualTo(expected)
    }

    @Test
    fun `should reject blank address`() {
        // when/then
        assertThatThrownBy { aDepositAddress(address = " ") }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("address must not be blank")
    }
}
