package com.stablecoin.custody.fireblocks.domain.transaction

import com.stablecoin.custody.fireblocks.domain.allocation.FundAllocationService
import com.stablecoin.custody.fireblocks.domain.audit.AuditLog
import com.stablecoin.custody.fireblocks.domain.audit.AuditLogRepository
import com.stablecoin.custody.fireblocks.domain.audit.AuditOperation
import com.stablecoin.custody.fireblocks.domain.audit.AuditStatus
import com.stablecoin.custody.fireblocks.domain.event.TransactionStatusChangedEvent
import com.stablecoin.custody.fireblocks.domain.port.EventPublisher
import com.stablecoin.custody.fireblocks.domain.reconciliation.InternalBalance
import com.stablecoin.custody.fireblocks.domain.reconciliation.InternalBalanceRepository
import com.stablecoin.custody.fireblocks.domain.shared.StateMachine
import com.stablecoin.custody.fireblocks.domain.shared.logger
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

private val log = logger<TransactionStatusHandler>()

@Service
class TransactionStatusHandler(
    private val transactionRepository: TransactionRepository,
    private val eventPublisher: EventPublisher<TransactionStatusChangedEvent>,
    private val auditLogRepository: AuditLogRepository,
    private val stateMachine: StateMachine<TransactionStatus, Transaction>,
    private val fundAllocationService: FundAllocationService,
    private val internalBalanceRepository: InternalBalanceRepository,
) {
    @Transactional
    fun handleStatusUpdate(
        fireblocksTxId: String,
        fireblocksStatus: String,
        subStatus: String?,
        txHash: String?,
    ) {
        val transaction = transactionRepository.findByFireblocksTransactionId(fireblocksTxId)
        if (transaction == null) {
            log.warn("Transaction not found for fireblocksTxId={}, skipping", fireblocksTxId)
            return
        }

        val newStatus = TransactionStatus.fromFireblocksStatus(fireblocksStatus)

        val locked =
            transactionRepository.findByIdForUpdate(transaction.id)
                ?: return

        if (locked.status == newStatus) {
            log.info("Transaction {} already in status {}, skipping", fireblocksTxId, newStatus)
            return
        }

        if (locked.status.terminal) {
            log.warn("Transaction {} already in terminal status {}, cannot update", fireblocksTxId, locked.status)
            return
        }

        stateMachine.transition(locked, newStatus)

        val previousStatus = locked.status
        val updated = locked.updateStatus(newStatus, fireblocksStatus, subStatus, txHash)
        val result = transactionRepository.save(updated)

        if (newStatus == TransactionStatus.FAILED) {
            fundAllocationService.releaseByTransactionId(result.id.value)
        }

        if (newStatus == TransactionStatus.CONFIRMED) {
            debitInternalBalance(result)
        }

        eventPublisher.publish(
            TransactionStatusChangedEvent(
                transactionId = result.id.value,
                externalTxId = result.externalTxId,
                previousStatus = previousStatus.name,
                newStatus = newStatus.name,
                fireblocksStatus = fireblocksStatus,
                subStatus = subStatus,
                txHash = txHash,
                occurredAt = Instant.now(),
            ),
        )

        auditLogRepository.save(
            AuditLog.create(
                operation = AuditOperation.TRANSACTION_STATUS_UPDATED,
                actor = "system",
                resourceId = result.id.value.toString(),
                status = AuditStatus.SUCCESS,
                details =
                    mapOf(
                        "previousStatus" to previousStatus.name,
                        "newStatus" to newStatus.name,
                        "fireblocksStatus" to fireblocksStatus,
                    ),
            ),
        )

        log.info("Updated transaction {} status: {} -> {}", fireblocksTxId, previousStatus, newStatus)
    }

    private fun debitInternalBalance(transaction: Transaction) {
        val vaultId = UUID.fromString(transaction.sourceVaultId)
        val existing =
            internalBalanceRepository.findByVaultIdAndCurrencyAndProtocol(
                vaultId,
                transaction.currency,
                transaction.protocol,
            )

        val updated =
            if (existing != null) {
                existing.debit(transaction.amount, transaction.id.value)
            } else {
                InternalBalance(
                    id = UUID.randomUUID(),
                    vaultId = vaultId,
                    currency = transaction.currency,
                    protocol = transaction.protocol,
                    balance = BigDecimal.ZERO.subtract(transaction.amount),
                    lastTransactionId = transaction.id.value,
                    updatedAt = Instant.now(),
                )
            }

        internalBalanceRepository.save(updated)
        log.info(
            "Debited internal balance for vault={} asset={}/{} amount={}",
            vaultId,
            transaction.currency,
            transaction.protocol,
            transaction.amount,
        )
    }
}
