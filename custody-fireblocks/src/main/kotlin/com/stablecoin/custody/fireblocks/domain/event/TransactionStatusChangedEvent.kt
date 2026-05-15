package com.stablecoin.custody.fireblocks.domain.event

import java.time.Instant
import java.util.UUID

data class TransactionStatusChangedEvent(
    val transactionId: UUID,
    val externalTxId: String,
    val previousStatus: String,
    val newStatus: String,
    val fireblocksStatus: String?,
    val subStatus: String?,
    val txHash: String?,
    val occurredAt: Instant,
) {
    companion object {
        const val TOPIC = "custody.transaction.status-changed"
    }
}
