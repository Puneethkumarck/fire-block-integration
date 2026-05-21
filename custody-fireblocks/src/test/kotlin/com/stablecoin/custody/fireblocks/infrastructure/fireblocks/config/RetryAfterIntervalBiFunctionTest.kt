package com.stablecoin.custody.fireblocks.infrastructure.fireblocks.config

import com.stablecoin.custody.fireblocks.infrastructure.fireblocks.client.FireblocksRateLimitException
import io.github.resilience4j.core.functions.Either
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.io.IOException

class RetryAfterIntervalBiFunctionTest {
    private val function = RetryAfterIntervalBiFunction()

    @Test
    fun `should use exponential backoff when exception is not rate limit`() {
        // given
        val exception = Either.left<Throwable, Any>(IOException("test"))

        // when
        val attempt1 = function.apply(1, exception)
        val attempt2 = function.apply(2, exception)
        val attempt3 = function.apply(3, exception)

        // then
        assertThat(attempt1).isEqualTo(500L)
        assertThat(attempt2).isEqualTo(1000L)
        assertThat(attempt3).isEqualTo(2000L)
    }

    @Test
    fun `should use exponential backoff when retry after is null`() {
        // given
        val exception =
            Either.left<Throwable, Any>(
                FireblocksRateLimitException(null, RuntimeException()),
            )

        // when
        val result = function.apply(1, exception)

        // then
        assertThat(result).isEqualTo(500L)
    }

    @Test
    fun `should use retry after when it exceeds own backoff`() {
        // given
        val exception =
            Either.left<Throwable, Any>(
                FireblocksRateLimitException(5L, RuntimeException()),
            )

        // when
        val result = function.apply(1, exception)

        // then
        assertThat(result).isEqualTo(5000L)
    }

    @Test
    fun `should use own backoff when it exceeds retry after`() {
        // given
        val exception =
            Either.left<Throwable, Any>(
                FireblocksRateLimitException(1L, RuntimeException()),
            )

        // when
        val result = function.apply(3, exception)

        // then
        assertThat(result).isEqualTo(2000L)
    }

    @Test
    fun `should use exponential backoff when either is right`() {
        // given
        val right = Either.right<Throwable, Any>(Any())

        // when
        val result = function.apply(1, right)

        // then
        assertThat(result).isEqualTo(500L)
    }
}
