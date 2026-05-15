package com.stablecoin.custody.fireblocks.domain.wallet

import java.time.Instant
import java.util.UUID

data class SupportedAsset(
    val id: UUID,
    val currency: String,
    val protocol: String,
    val fireblocksAssetId: String,
    val taggable: Boolean,
    val utxo: Boolean,
    val active: Boolean,
    val createdAt: Instant,
) {
    init {
        require(currency.isNotBlank()) { "currency must not be blank" }
        require(protocol.isNotBlank()) { "protocol must not be blank" }
        require(fireblocksAssetId.isNotBlank()) { "fireblocksAssetId must not be blank" }
    }
}
