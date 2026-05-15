package com.stablecoin.custody.fireblocks.domain.exception

import com.stablecoin.custody.fireblocks.domain.shared.CustodyException
import com.stablecoin.custody.fireblocks.domain.shared.ErrorCode

class FireblocksTimeoutException(
    message: String,
    cause: Throwable? = null,
) : CustodyException(ErrorCode.FIREBLOCKS_TIMEOUT, message, cause)
