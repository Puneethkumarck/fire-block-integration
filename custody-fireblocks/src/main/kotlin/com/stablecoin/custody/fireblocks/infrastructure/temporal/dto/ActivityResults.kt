package com.stablecoin.custody.fireblocks.infrastructure.temporal.dto

data class CreateTransactionResult(
    val transactionId: String,
)

data class SubmitResult(
    val fireblocksTransactionId: String,
    val status: String,
    val subStatus: String?,
    val txHash: String?,
)

data class PollStatusResult(
    val fireblocksStatus: String,
    val subStatus: String?,
    val txHash: String?,
)

data class ReserveFundsResult(
    val allocationId: String,
)
