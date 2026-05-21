package com.stablecoin.custody.fireblocks.infrastructure.fireblocks.client

class FireblocksRateLimitException(
    val retryAfterSeconds: Long?,
    cause: Throwable,
) : RuntimeException("Fireblocks rate limit exceeded", cause)
