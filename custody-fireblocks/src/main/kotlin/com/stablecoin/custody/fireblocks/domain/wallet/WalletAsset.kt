package com.stablecoin.custody.fireblocks.domain.wallet

import com.stablecoin.custody.fireblocks.domain.vault.VaultId
import java.time.Instant
import java.util.UUID

data class WalletAsset(
    val id: WalletAssetId,
    val vaultId: VaultId,
    val currency: String,
    val protocol: String,
    val fireblocksAssetId: String,
    val status: AssetStatus,
    val createdAt: Instant,
    val updatedAt: Instant,
    val version: Long = 0,
) {
    init {
        require(currency.isNotBlank()) { "currency must not be blank" }
        require(protocol.isNotBlank()) { "protocol must not be blank" }
        require(fireblocksAssetId.isNotBlank()) { "fireblocksAssetId must not be blank" }
    }

    companion object {
        fun create(
            vaultId: VaultId,
            currency: String,
            protocol: String,
            fireblocksAssetId: String,
        ): WalletAsset {
            val now = Instant.now()
            return WalletAsset(
                id = WalletAssetId(UUID.randomUUID()),
                vaultId = vaultId,
                currency = currency,
                protocol = protocol,
                fireblocksAssetId = fireblocksAssetId,
                status = AssetStatus.ACTIVE,
                createdAt = now,
                updatedAt = now,
            )
        }
    }
}
