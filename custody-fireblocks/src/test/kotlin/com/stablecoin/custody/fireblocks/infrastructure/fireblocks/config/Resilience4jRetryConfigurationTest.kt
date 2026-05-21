package com.stablecoin.custody.fireblocks.infrastructure.fireblocks.config

import com.stablecoin.custody.fireblocks.infrastructure.fireblocks.client.FireblocksRateLimitException
import io.github.resilience4j.core.functions.Either
import io.github.resilience4j.retry.Retry
import io.github.resilience4j.retry.RetryConfig
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class Resilience4jRetryConfigurationTest {
    private val configuration = Resilience4jRetryConfiguration()

    @Test
    fun `should create customizer for fireblocks instance`() {
        // given
        val customizer = configuration.fireblocksRetryConfigCustomizer()

        // when
        val name = customizer.name()

        // then
        assertThat(name).isEqualTo("fireblocks")
    }

    @Test
    fun `should apply retry after interval function to retry config`() {
        // given
        val customizer = configuration.fireblocksRetryConfigCustomizer()
        val builder =
            RetryConfig
                .custom<Any>()
                .retryOnException { it is FireblocksRateLimitException }

        // when
        customizer.customize(builder)
        val config = builder.build()
        val retry = Retry.of("test", config)

        // then
        val interval =
            retry.retryConfig
                .getIntervalBiFunction<Any>()
                .apply(1, Either.left(FireblocksRateLimitException(5L, RuntimeException())))
        assertThat(interval).isEqualTo(5000L)
    }
}
