package com.stablecoin.custody.fireblocks.domain.wallet

import com.stablecoin.custody.fireblocks.domain.vault.VaultId
import com.stablecoin.custody.fireblocks.test.fixtures.aWalletAsset
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.util.UUID

class WalletAssetTest {
    @Test
    fun `should create wallet asset in ACTIVE status`() {
        // given
        val vaultId = VaultId(UUID.randomUUID())

        // when
        val result =
            WalletAsset.create(
                vaultId = vaultId,
                currency = "ETH",
                protocol = "ETH",
                fireblocksAssetId = "ETH",
            )

        // then
        val expected =
            aWalletAsset(
                vaultId = vaultId,
                currency = "ETH",
                protocol = "ETH",
                fireblocksAssetId = "ETH",
                status = AssetStatus.ACTIVE,
            )
        assertThat(result)
            .usingRecursiveComparison()
            .ignoringFields("id", "createdAt", "updatedAt")
            .isEqualTo(expected)
    }

    @Test
    fun `should reject blank currency`() {
        // when/then
        assertThatThrownBy { aWalletAsset(currency = " ") }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("currency must not be blank")
    }

    @Test
    fun `should reject blank protocol`() {
        // when/then
        assertThatThrownBy { aWalletAsset(protocol = " ") }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("protocol must not be blank")
    }

    @Test
    fun `should reject blank fireblocksAssetId`() {
        // when/then
        assertThatThrownBy { aWalletAsset(fireblocksAssetId = " ") }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("fireblocksAssetId must not be blank")
    }
}
