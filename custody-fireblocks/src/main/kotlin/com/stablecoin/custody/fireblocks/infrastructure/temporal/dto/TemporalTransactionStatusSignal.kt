package com.stablecoin.custody.fireblocks.infrastructure.temporal.dto

import com.stablecoin.custody.fireblocks.domain.port.TransactionStatusSignal

data class TemporalTransactionStatusSignal(
    val fireblocksTransactionId: String,
    val fireblocksStatus: String,
    val subStatus: String?,
    val txHash: String?,
)

fun TransactionStatusSignal.toTemporalSignal() =
    TemporalTransactionStatusSignal(
        fireblocksTransactionId = fireblocksTransactionId,
        fireblocksStatus = fireblocksStatus,
        subStatus = subStatus,
        txHash = txHash,
    )
