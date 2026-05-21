package com.stablecoin.custody.fireblocks.domain.exception

import com.stablecoin.custody.fireblocks.domain.shared.CustodyException
import com.stablecoin.custody.fireblocks.domain.shared.ErrorCode

class TransactionNotCancellableException(
    transactionId: String,
) : CustodyException(
        ErrorCode.TRANSACTION_NOT_CANCELLABLE,
        "Transaction $transactionId is no longer cancellable",
    )
