package com.stablecoin.custody.fireblocks.infrastructure.temporal

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "temporal")
data class TemporalProperties(
    val serviceAddress: String,
    val namespace: String,
)
