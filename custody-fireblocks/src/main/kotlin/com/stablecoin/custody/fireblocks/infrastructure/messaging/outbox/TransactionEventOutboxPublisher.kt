package com.stablecoin.custody.fireblocks.infrastructure.messaging.outbox

import com.stablecoin.custody.fireblocks.domain.event.TransactionStatusChangedEvent
import com.stablecoin.custody.fireblocks.domain.port.EventPublisher
import io.namastack.outbox.Outbox
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Component
class TransactionEventOutboxPublisher(
    outbox: Outbox,
) : AbstractOutboxEventPublisher(outbox, listOf("externalTxId")),
    EventPublisher<TransactionStatusChangedEvent> {
    @Transactional(propagation = Propagation.MANDATORY)
    override fun publish(event: TransactionStatusChangedEvent) {
        schedule(event)
    }
}
