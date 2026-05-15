package com.stablecoin.custody.fireblocks.infrastructure.fireblocks.config

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties(prefix = "fireblocks")
data class FireblocksProperties(
    val api: ApiProperties,
    val secrets: SecretsProperties,
) {
    data class ApiProperties(
        val baseUrl: String,
        val connectTimeout: Duration,
        val readTimeout: Duration,
    )

    data class SecretsProperties(
        val privateKeyName: String,
        val apiKeyName: String,
        val webhookPublicKeyName: String,
    )
}
