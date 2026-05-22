package com.stablecoin.custody.fireblocks.domain.reconciliation

import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

data class ReconciliationResult(
    val id: UUID,
    val vaultId: UUID,
    val currency: String,
    val protocol: String,
    val internalBalance: BigDecimal,
    val fireblocksBalance: BigDecimal,
    val drift: BigDecimal,
    val absoluteDrift: BigDecimal,
    val status: ReconciliationStatus,
    val toleranceUsed: BigDecimal,
    val createdAt: Instant,
)
