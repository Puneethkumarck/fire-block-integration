package com.stablecoin.custody.fireblocks.infrastructure.messaging.outbox

import com.stablecoin.custody.fireblocks.test.fixtures.aTransactionStatusChangedEvent
import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import io.namastack.outbox.Outbox
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class TransactionEventOutboxPublisherTest {
    private val outbox: Outbox = mockk()
    private val publisher = TransactionEventOutboxPublisher(outbox)

    @Test
    fun `should schedule transaction status changed event via outbox`() {
        // given
        val event = aTransactionStatusChangedEvent()
        every { outbox.schedule(any(), any<String>()) } returns Unit

        // when
        publisher.publish(event)

        // then
        verify { outbox.schedule(event, any<String>()) }
    }

    @Test
    fun `should use externalTxId as partition key`() {
        // given
        val event = aTransactionStatusChangedEvent()
        val keySlot = slot<String>()
        every { outbox.schedule(any(), capture(keySlot)) } returns Unit

        // when
        publisher.publish(event)

        // then
        assertThat(keySlot.captured).isEqualTo(event.externalTxId)
    }
}
