package com.stablecoin.custody.fireblocks.infrastructure.fireblocks.config

import com.stablecoin.custody.fireblocks.infrastructure.fireblocks.client.FireblocksRateLimitException
import io.github.resilience4j.core.IntervalBiFunction
import io.github.resilience4j.core.functions.Either
import kotlin.math.max
import kotlin.math.pow

class RetryAfterIntervalBiFunction(
    private val baseWaitMs: Long = 500L,
    private val multiplier: Double = 2.0,
) : IntervalBiFunction<Any> {
    override fun apply(
        attempt: Int,
        either: Either<Throwable, Any>,
    ): Long {
        val ownBackoff = (baseWaitMs * multiplier.pow((attempt - 1).toDouble())).toLong()

        val retryAfterMs =
            either.fold(
                { throwable ->
                    (throwable as? FireblocksRateLimitException)
                        ?.retryAfterSeconds
                        ?.let { it * 1000L }
                },
                { null },
            )

        return if (retryAfterMs != null) max(ownBackoff, retryAfterMs) else ownBackoff
    }
}
