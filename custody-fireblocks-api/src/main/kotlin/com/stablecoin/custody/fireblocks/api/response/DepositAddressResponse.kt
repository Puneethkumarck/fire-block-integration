package com.stablecoin.custody.fireblocks.api.response

import java.time.Instant
import java.util.UUID

data class DepositAddressResponse(
    val id: UUID,
    val walletAssetId: UUID,
    val address: String,
    val tag: String?,
    val legacyAddress: String?,
    val createdAt: Instant,
)
