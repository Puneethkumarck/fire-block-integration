package com.stablecoin.custody.fireblocks.application.stream

import com.stablecoin.custody.fireblocks.domain.event.TransactionStatusChangedEvent
import com.stablecoin.custody.fireblocks.domain.exception.InvalidTransactionStateException
import com.stablecoin.custody.fireblocks.domain.shared.logger
import com.stablecoin.custody.fireblocks.domain.transaction.TransactionStatusUpdateHandler
import org.springframework.kafka.annotation.BackOff
import org.springframework.kafka.annotation.DltHandler
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.annotation.RetryableTopic
import org.springframework.kafka.retrytopic.DltStrategy
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.messaging.handler.annotation.Header
import org.springframework.stereotype.Component

private val log = logger<TransactionStatusEventConsumer>()

@Component
class TransactionStatusEventConsumer(
    private val handler: TransactionStatusUpdateHandler,
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
    fun consume(event: TransactionStatusChangedEvent) {
        log.info(
            "Received transaction status change: transactionId={}, newStatus={}",
            event.transactionId,
            event.newStatus,
        )
        handler.handle(event)
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
