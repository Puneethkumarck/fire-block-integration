package com.stablecoin.custody.fireblocks.domain.exception

import com.stablecoin.custody.fireblocks.domain.shared.CustodyException
import com.stablecoin.custody.fireblocks.domain.shared.ErrorCode

class TransactionDuplicateException(
    externalTxId: String,
) : CustodyException(ErrorCode.TRANSACTION_DUPLICATE, "Transaction already exists with externalTxId: $externalTxId")
