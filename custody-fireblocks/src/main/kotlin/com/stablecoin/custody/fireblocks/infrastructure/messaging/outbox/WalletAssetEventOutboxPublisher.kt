package com.stablecoin.custody.fireblocks.infrastructure.messaging.outbox

import com.stablecoin.custody.fireblocks.domain.event.WalletAssetCreatedEvent
import com.stablecoin.custody.fireblocks.domain.port.EventPublisher
import io.namastack.outbox.Outbox
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Component
class WalletAssetEventOutboxPublisher(
    outbox: Outbox,
) : AbstractOutboxEventPublisher(outbox, listOf("vaultId")),
    EventPublisher<WalletAssetCreatedEvent> {
    @Transactional(propagation = Propagation.MANDATORY)
    override fun publish(event: WalletAssetCreatedEvent) {
        schedule(event)
    }
}
