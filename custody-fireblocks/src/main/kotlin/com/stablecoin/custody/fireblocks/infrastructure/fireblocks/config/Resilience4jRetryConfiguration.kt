package com.stablecoin.custody.fireblocks.infrastructure.fireblocks.config

import io.github.resilience4j.common.retry.configuration.RetryConfigCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class Resilience4jRetryConfiguration {
    @Bean
    fun fireblocksRetryConfigCustomizer() =
        RetryConfigCustomizer.of("fireblocks") { builder ->
            builder.intervalBiFunction(RetryAfterIntervalBiFunction())
        }
}
