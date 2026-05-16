package com.stablecoin.custody.fireblocks.api.response

import java.time.Instant
import java.util.UUID

data class VaultResponse(
    val id: UUID,
    val fireblocksVaultId: String?,
    val customerRefId: String,
    val name: String,
    val status: String,
    val createdAt: Instant,
    val updatedAt: Instant,
)
