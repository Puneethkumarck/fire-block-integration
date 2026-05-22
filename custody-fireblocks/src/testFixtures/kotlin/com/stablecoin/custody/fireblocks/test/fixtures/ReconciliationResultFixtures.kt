package com.stablecoin.custody.fireblocks.test.fixtures

import com.stablecoin.custody.fireblocks.domain.reconciliation.ReconciliationResult
import com.stablecoin.custody.fireblocks.domain.reconciliation.ReconciliationStatus
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

fun aReconciliationResult(
    id: UUID = UUID.randomUUID(),
    vaultId: UUID = UUID.randomUUID(),
    currency: String = "EURC",
    protocol: String = "ETH",
    internalBalance: BigDecimal = BigDecimal("1000.00"),
    fireblocksBalance: BigDecimal = BigDecimal("1000.00"),
    drift: BigDecimal = BigDecimal.ZERO,
    absoluteDrift: BigDecimal = BigDecimal.ZERO,
    status: ReconciliationStatus = ReconciliationStatus.MATCHED,
    toleranceUsed: BigDecimal = BigDecimal("0.01"),
    createdAt: Instant = Instant.now(),
) = ReconciliationResult(
    id = id,
    vaultId = vaultId,
    currency = currency,
    protocol = protocol,
    internalBalance = internalBalance,
    fireblocksBalance = fireblocksBalance,
    drift = drift,
    absoluteDrift = absoluteDrift,
    status = status,
    toleranceUsed = toleranceUsed,
    createdAt = createdAt,
)
