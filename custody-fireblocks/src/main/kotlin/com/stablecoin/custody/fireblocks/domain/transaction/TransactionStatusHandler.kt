package com.stablecoin.custody.fireblocks.domain.transaction

import com.stablecoin.custody.fireblocks.domain.audit.AuditLog
import com.stablecoin.custody.fireblocks.domain.audit.AuditLogRepository
import com.stablecoin.custody.fireblocks.domain.audit.AuditOperation
import com.stablecoin.custody.fireblocks.domain.audit.AuditStatus
import com.stablecoin.custody.fireblocks.domain.event.TransactionStatusChangedEvent
import com.stablecoin.custody.fireblocks.domain.port.EventPublisher
import com.stablecoin.custody.fireblocks.domain.shared.StateMachine
import com.stablecoin.custody.fireblocks.domain.shared.logger
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

private val log = logger<TransactionStatusHandler>()

@Service
class TransactionStatusHandler(
    private val transactionRepository: TransactionRepository,
    private val eventPublisher: EventPublisher<TransactionStatusChangedEvent>,
    private val auditLogRepository: AuditLogRepository,
    private val stateMachine: StateMachine<TransactionStatus, Transaction>,
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

        if (transaction.status == newStatus) {
            log.info("Transaction {} already in status {}, skipping", fireblocksTxId, newStatus)
            return
        }

        if (transaction.status.terminal) {
            log.error("Transaction {} already in terminal status {}, cannot update", fireblocksTxId, transaction.status)
            return
        }

        stateMachine.transition(transaction, newStatus)

        val locked =
            transactionRepository.findByIdForUpdate(transaction.id)
                ?: return

        val previousStatus = locked.status
        val updated = locked.updateStatus(newStatus, fireblocksStatus, subStatus, txHash)
        val result = transactionRepository.save(updated)

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
}
