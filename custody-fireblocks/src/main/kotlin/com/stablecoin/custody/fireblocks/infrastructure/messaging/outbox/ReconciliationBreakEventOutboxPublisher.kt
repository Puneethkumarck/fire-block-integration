package com.stablecoin.custody.fireblocks.infrastructure.messaging.outbox

import com.stablecoin.custody.fireblocks.domain.event.ReconciliationBreakDetectedEvent
import com.stablecoin.custody.fireblocks.domain.port.EventPublisher
import io.namastack.outbox.Outbox
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Component
class ReconciliationBreakEventOutboxPublisher(
    outbox: Outbox,
) : AbstractOutboxEventPublisher(outbox, listOf("vaultId")),
    EventPublisher<ReconciliationBreakDetectedEvent> {
    @Transactional(propagation = Propagation.REQUIRED)
    override fun publish(event: ReconciliationBreakDetectedEvent) {
        schedule(event)
    }
}
