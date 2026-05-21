package com.stablecoin.custody.fireblocks.domain.exception

import com.stablecoin.custody.fireblocks.domain.shared.CustodyException
import com.stablecoin.custody.fireblocks.domain.shared.ErrorCode

class TransactionAlreadyTerminalException(
    transactionId: String,
    status: String,
) : CustodyException(
        ErrorCode.TRANSACTION_ALREADY_TERMINAL,
        "Transaction $transactionId is already in terminal state $status",
    )
