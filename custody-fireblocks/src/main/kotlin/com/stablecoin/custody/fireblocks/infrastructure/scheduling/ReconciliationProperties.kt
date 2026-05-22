package com.stablecoin.custody.fireblocks.infrastructure.scheduling

import org.springframework.boot.context.properties.ConfigurationProperties
import java.math.BigDecimal

@ConfigurationProperties(prefix = "custody.reconciliation")
data class ReconciliationProperties(
    val enabled: Boolean = true,
    val interval: Long = 900000,
    val defaultTolerance: BigDecimal = BigDecimal("0.01"),
    val tolerances: Map<String, BigDecimal> = emptyMap(),
)
