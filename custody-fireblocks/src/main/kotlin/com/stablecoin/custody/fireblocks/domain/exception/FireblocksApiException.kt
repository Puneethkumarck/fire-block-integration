package com.stablecoin.custody.fireblocks.domain.exception

import com.stablecoin.custody.fireblocks.domain.shared.CustodyException
import com.stablecoin.custody.fireblocks.domain.shared.ErrorCode

class FireblocksApiException(
    message: String,
    cause: Throwable? = null,
) : CustodyException(ErrorCode.FIREBLOCKS_API_ERROR, message, cause)
