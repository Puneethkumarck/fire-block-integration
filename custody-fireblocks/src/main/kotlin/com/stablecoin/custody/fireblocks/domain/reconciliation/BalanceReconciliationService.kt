package com.stablecoin.custody.fireblocks.domain.reconciliation

import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@Service
class BalanceReconciliationService {
    fun reconcile(
        internalBalance: InternalBalance,
        fireblocksAvailable: BigDecimal,
        tolerance: BigDecimal,
    ): ReconciliationResult {
        val drift = fireblocksAvailable.subtract(internalBalance.balance)
        val absoluteDrift = drift.abs()
        val status = if (absoluteDrift <= tolerance) ReconciliationStatus.MATCHED else ReconciliationStatus.MISMATCHED

        return ReconciliationResult(
            id = UUID.randomUUID(),
            vaultId = internalBalance.vaultId,
            currency = internalBalance.currency,
            protocol = internalBalance.protocol,
            internalBalance = internalBalance.balance,
            fireblocksBalance = fireblocksAvailable,
            drift = drift,
            absoluteDrift = absoluteDrift,
            status = status,
            toleranceUsed = tolerance,
            createdAt = Instant.now(),
        )
    }

    fun createPartialResult(
        vaultId: UUID,
        currency: String,
        protocol: String,
        internalBalance: BigDecimal,
        tolerance: BigDecimal,
    ): ReconciliationResult =
        ReconciliationResult(
            id = UUID.randomUUID(),
            vaultId = vaultId,
            currency = currency,
            protocol = protocol,
            internalBalance = internalBalance,
            fireblocksBalance = BigDecimal.ZERO,
            drift = BigDecimal.ZERO,
            absoluteDrift = BigDecimal.ZERO,
            status = ReconciliationStatus.PARTIAL,
            toleranceUsed = tolerance,
            createdAt = Instant.now(),
        )
}
