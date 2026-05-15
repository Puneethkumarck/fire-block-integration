package com.stablecoin.custody.fireblocks.domain.exception

import com.stablecoin.custody.fireblocks.domain.shared.CustodyException
import com.stablecoin.custody.fireblocks.domain.shared.ErrorCode

class InsufficientBalanceException(
    vaultId: String,
    assetId: String,
) : CustodyException(ErrorCode.INSUFFICIENT_BALANCE, "Insufficient balance for asset $assetId in vault $vaultId")
