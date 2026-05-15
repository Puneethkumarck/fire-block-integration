package com.stablecoin.custody.fireblocks.infrastructure.security

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties(prefix = "rate-limit")
data class RateLimitProperties(
    val capacity: Long = 100,
    val refillTokens: Long = 100,
    val refillDuration: Duration = Duration.ofMinutes(1),
)
