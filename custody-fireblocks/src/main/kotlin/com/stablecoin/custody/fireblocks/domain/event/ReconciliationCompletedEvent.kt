package com.stablecoin.custody.fireblocks.domain.event

import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

data class ReconciliationCompletedEvent(
    val vaultId: UUID,
    val currency: String,
    val protocol: String,
    val internalBalance: BigDecimal,
    val fireblocksBalance: BigDecimal,
    val drift: BigDecimal,
    val toleranceUsed: BigDecimal,
    val occurredAt: Instant,
) {
    companion object {
        const val TOPIC = "custody.reconciliation.completed"
    }
}
