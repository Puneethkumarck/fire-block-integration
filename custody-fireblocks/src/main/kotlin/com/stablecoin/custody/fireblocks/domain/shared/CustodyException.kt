package com.stablecoin.custody.fireblocks.domain.shared

abstract class CustodyException(
    val errorCode: ErrorCode,
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)
