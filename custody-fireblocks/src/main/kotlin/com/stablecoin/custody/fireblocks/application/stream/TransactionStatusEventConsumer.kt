package com.stablecoin.custody.fireblocks.application.stream

import com.stablecoin.custody.fireblocks.domain.event.TransactionStatusChangedEvent
import com.stablecoin.custody.fireblocks.domain.exception.InvalidTransactionStateException
import com.stablecoin.custody.fireblocks.domain.shared.logger
import com.stablecoin.custody.fireblocks.domain.transaction.TransactionRepository
import com.stablecoin.custody.fireblocks.domain.transaction.TransactionStatus
import org.springframework.kafka.annotation.BackOff
import org.springframework.kafka.annotation.DltHandler
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.annotation.RetryableTopic
import org.springframework.kafka.retrytopic.DltStrategy
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.messaging.handler.annotation.Header
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

private val log = logger<TransactionStatusEventConsumer>()

@Component
class TransactionStatusEventConsumer(
    private val transactionRepository: TransactionRepository,
) {
    @RetryableTopic(
        attempts = "4",
        backOff = BackOff(delay = 1000, multiplier = 5.0, maxDelay = 30000),
        dltStrategy = DltStrategy.ALWAYS_RETRY_ON_ERROR,
        autoCreateTopics = "true",
        exclude = [InvalidTransactionStateException::class, IllegalArgumentException::class],
    )
    @KafkaListener(
        topics = [TransactionStatusChangedEvent.TOPIC],
        groupId = "custody-fireblocks",
    )
    @Transactional
    fun consume(event: TransactionStatusChangedEvent) {
        log.info(
            "Received transaction status change: transactionId={}, newStatus={}",
            event.transactionId,
            event.newStatus,
        )

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

    @DltHandler
    fun handleDlt(
        event: TransactionStatusChangedEvent,
        @Header(KafkaHeaders.RECEIVED_TOPIC) topic: String,
    ) {
        log.error(
            "Message exhausted retries, sent to DLT: topic={}, transactionId={}, externalTxId={}, newStatus={}",
            topic,
            event.transactionId,
            event.externalTxId,
            event.newStatus,
        )
    }
}
