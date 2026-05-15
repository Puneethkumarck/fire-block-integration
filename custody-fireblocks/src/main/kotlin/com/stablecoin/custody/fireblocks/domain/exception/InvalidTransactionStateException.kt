package com.stablecoin.custody.fireblocks.domain.exception

import com.stablecoin.custody.fireblocks.domain.shared.CustodyException
import com.stablecoin.custody.fireblocks.domain.shared.ErrorCode

class InvalidTransactionStateException(
    transactionId: String,
    currentStatus: String,
    targetStatus: String,
) : CustodyException(
        ErrorCode.INVALID_TRANSACTION_STATE,
        "Transaction $transactionId cannot transition from $currentStatus to $targetStatus",
    )
