package com.stablecoin.custody.fireblocks.domain.exception

import com.stablecoin.custody.fireblocks.domain.shared.CustodyException
import com.stablecoin.custody.fireblocks.domain.shared.ErrorCode

class TransactionNotFoundException(
    externalTxId: String,
) : CustodyException(ErrorCode.TRANSACTION_NOT_FOUND, "Transaction not found for externalTxId: $externalTxId")
