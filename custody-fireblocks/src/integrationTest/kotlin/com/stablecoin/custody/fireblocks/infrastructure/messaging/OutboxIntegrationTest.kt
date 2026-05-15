package com.stablecoin.custody.fireblocks.infrastructure.messaging

import com.stablecoin.custody.fireblocks.AbstractIntegrationTest
import com.stablecoin.custody.fireblocks.domain.event.VaultCreatedEvent
import com.stablecoin.custody.fireblocks.domain.port.EventPublisher
import com.stablecoin.custody.fireblocks.test.fixtures.aVaultCreatedEvent
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.annotation.Transactional

class OutboxIntegrationTest : AbstractIntegrationTest() {
    @Autowired
    private lateinit var vaultEventPublisher: EventPublisher<VaultCreatedEvent>

    @Test
    @Transactional
    fun `should publish vault created event to Kafka via outbox`() {
        // given
        val event = aVaultCreatedEvent()

        // when
        vaultEventPublisher.publish(event)
    }
}
