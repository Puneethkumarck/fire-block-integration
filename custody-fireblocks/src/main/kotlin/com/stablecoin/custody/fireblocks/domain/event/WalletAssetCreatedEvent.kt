package com.stablecoin.custody.fireblocks.domain.event

import com.stablecoin.custody.fireblocks.domain.wallet.WalletAsset
import java.time.Instant
import java.util.UUID

data class WalletAssetCreatedEvent(
    val walletAssetId: UUID,
    val vaultId: UUID,
    val currency: String,
    val protocol: String,
    val fireblocksAssetId: String,
    val occurredAt: Instant,
) {
    companion object {
        const val TOPIC = "custody.wallet-asset.activated"

        fun from(walletAsset: WalletAsset) =
            WalletAssetCreatedEvent(
                walletAssetId = walletAsset.id.value,
                vaultId = walletAsset.vaultId.value,
                currency = walletAsset.currency,
                protocol = walletAsset.protocol,
                fireblocksAssetId = walletAsset.fireblocksAssetId,
                occurredAt = Instant.now(),
            )
    }
}
