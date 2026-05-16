package com.stablecoin.custody.fireblocks.api.response

import java.time.Instant
import java.util.UUID

data class WalletAssetResponse(
    val id: UUID,
    val vaultId: UUID,
    val currency: String,
    val protocol: String,
    val fireblocksAssetId: String,
    val status: String,
    val createdAt: Instant,
    val updatedAt: Instant,
)
