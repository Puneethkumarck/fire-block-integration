package com.stablecoin.custody.fireblocks.infrastructure.temporal

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "temporal")
data class TemporalProperties(
    val serviceAddress: String,
    val namespace: String,
) {
    init {
        require(serviceAddress.isNotBlank()) { "temporal.service-address must not be blank" }
        require(namespace.isNotBlank()) { "temporal.namespace must not be blank" }
    }
}
