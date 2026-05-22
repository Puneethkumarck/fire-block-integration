package com.stablecoin.custody.fireblocks.domain.reconciliation

import com.stablecoin.custody.fireblocks.domain.audit.AuditLog
import com.stablecoin.custody.fireblocks.domain.audit.AuditLogRepository
import com.stablecoin.custody.fireblocks.domain.audit.AuditOperation
import com.stablecoin.custody.fireblocks.domain.audit.AuditStatus
import com.stablecoin.custody.fireblocks.domain.event.ReconciliationBreakDetectedEvent
import com.stablecoin.custody.fireblocks.domain.port.EventPublisher
import com.stablecoin.custody.fireblocks.domain.shared.logger
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

private val log = logger<BalanceReconciliationService>()

@Service
class BalanceReconciliationService(
    private val reconciliationResultRepository: ReconciliationResultRepository,
    private val internalBalanceRepository: InternalBalanceRepository,
    private val auditLogRepository: AuditLogRepository,
    private val breakEventPublisher: EventPublisher<ReconciliationBreakDetectedEvent>,
) {
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

    @Transactional
    fun persistResult(result: ReconciliationResult) {
        reconciliationResultRepository.save(result)
        auditReconciliation(result)

        if (result.status == ReconciliationStatus.MISMATCHED) {
            publishBreakEvent(result)
        }
    }

    @Transactional
    fun seedAndPersist(
        vaultId: UUID,
        currency: String,
        protocol: String,
        fireblocksAvailable: BigDecimal,
        tolerance: BigDecimal,
    ) {
        val seeded =
            InternalBalance(
                id = UUID.randomUUID(),
                vaultId = vaultId,
                currency = currency,
                protocol = protocol,
                balance = fireblocksAvailable,
                lastTransactionId = null,
                updatedAt = Instant.now(),
            )
        internalBalanceRepository.save(seeded)

        val matchedResult = reconcile(seeded, fireblocksAvailable, tolerance)
        reconciliationResultRepository.save(matchedResult)
        auditReconciliation(matchedResult)

        log.info(
            "Seeded internal balance for vault={} asset={}/{} balance={}",
            vaultId,
            currency,
            protocol,
            fireblocksAvailable,
        )
    }

    private fun auditReconciliation(result: ReconciliationResult) {
        val operation =
            when (result.status) {
                ReconciliationStatus.MATCHED -> AuditOperation.RECONCILIATION_MATCHED
                ReconciliationStatus.MISMATCHED -> AuditOperation.RECONCILIATION_MISMATCHED
                ReconciliationStatus.PARTIAL -> AuditOperation.RECONCILIATION_PARTIAL
            }

        auditLogRepository.save(
            AuditLog.create(
                operation = operation,
                actor = "system",
                resourceId = result.vaultId.toString(),
                status = if (result.status == ReconciliationStatus.MISMATCHED) AuditStatus.FAILURE else AuditStatus.SUCCESS,
                details =
                    mapOf(
                        "currency" to result.currency,
                        "protocol" to result.protocol,
                        "drift" to result.drift.toPlainString(),
                        "status" to result.status.name,
                    ),
            ),
        )
    }

    private fun publishBreakEvent(result: ReconciliationResult) {
        breakEventPublisher.publish(
            ReconciliationBreakDetectedEvent(
                vaultId = result.vaultId,
                currency = result.currency,
                protocol = result.protocol,
                internalBalance = result.internalBalance,
                fireblocksBalance = result.fireblocksBalance,
                drift = result.drift,
                absoluteDrift = result.absoluteDrift,
                toleranceUsed = result.toleranceUsed,
                occurredAt = result.createdAt,
            ),
        )

        log.warn(
            "Reconciliation break detected: vault={} asset={}/{} drift={}",
            result.vaultId,
            result.currency,
            result.protocol,
            result.drift,
        )
    }
}
