package com.stablecoin.custody.fireblocks.domain.exception

class InvalidTransactionStateException(
    transactionId: String,
    currentStatus: String,
    targetStatus: String,
) : RuntimeException("Transaction $transactionId cannot transition from $currentStatus to $targetStatus")
