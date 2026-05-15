package com.stablecoin.custody.fireblocks.domain.exception

import com.stablecoin.custody.fireblocks.domain.shared.CustodyException
import com.stablecoin.custody.fireblocks.domain.shared.ErrorCode

class OutboxPublishException(
    message: String,
    cause: Throwable? = null,
) : CustodyException(ErrorCode.OUTBOX_PUBLISH_FAILED, message, cause)
