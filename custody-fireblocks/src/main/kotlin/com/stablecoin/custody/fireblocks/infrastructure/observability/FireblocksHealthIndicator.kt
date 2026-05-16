package com.stablecoin.custody.fireblocks.infrastructure.observability

import com.stablecoin.custody.fireblocks.infrastructure.fireblocks.client.FireblocksVaultClient
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.health.contributor.Health
import org.springframework.boot.health.contributor.HealthIndicator
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(name = ["fireblocks.health.enabled"], havingValue = "true", matchIfMissing = true)
class FireblocksHealthIndicator(
    private val fireblocksVaultClient: FireblocksVaultClient,
) : HealthIndicator {
    override fun health(): Health =
        try {
            fireblocksVaultClient.getVault("0")
            Health.up().build()
        } catch (e: Exception) {
            Health.down(e).build()
        }
}
