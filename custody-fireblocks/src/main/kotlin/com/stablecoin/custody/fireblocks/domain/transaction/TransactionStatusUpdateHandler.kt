package com.stablecoin.custody.fireblocks.domain.transaction

import com.stablecoin.custody.fireblocks.domain.event.TransactionStatusChangedEvent
import com.stablecoin.custody.fireblocks.domain.shared.logger
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

private val log = logger<TransactionStatusUpdateHandler>()

@Service
class TransactionStatusUpdateHandler(
    private val transactionRepository: TransactionRepository,
) {
    @Transactional
    fun handle(event: TransactionStatusChangedEvent) {
        val transaction = transactionRepository.findByExternalTxId(event.externalTxId)
        if (transaction == null) {
            log.warn("Transaction not found for externalTxId={}, skipping", event.externalTxId)
            return
        }

        val newStatus =
            TransactionStatus.entries.find { it.name == event.newStatus }
                ?: throw IllegalArgumentException("Unknown transaction status: ${event.newStatus}")

        if (transaction.status == newStatus) {
            log.info("Transaction {} already in status {}, skipping (idempotent)", event.transactionId, newStatus)
            return
        }

        if (transaction.status.terminal) {
            log.warn("Transaction {} already in terminal status {}, skipping", event.transactionId, transaction.status)
            return
        }

        val updated =
            transaction.updateStatus(
                newStatus = newStatus,
                fireblocksStatus = event.fireblocksStatus ?: event.newStatus,
                fireblocksSubStatus = event.subStatus,
                txHash = event.txHash,
            )
        transactionRepository.save(updated)
        log.info("Updated transaction {} status: {} -> {}", event.transactionId, event.previousStatus, event.newStatus)
    }
}
