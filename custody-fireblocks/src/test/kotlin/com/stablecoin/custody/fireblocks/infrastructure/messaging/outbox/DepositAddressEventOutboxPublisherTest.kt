package com.stablecoin.custody.fireblocks.infrastructure.messaging.outbox

import com.stablecoin.custody.fireblocks.test.fixtures.anAddressCreatedEvent
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
class DepositAddressEventOutboxPublisherTest {
    private val outbox: Outbox = mockk()
    private val publisher = DepositAddressEventOutboxPublisher(outbox)

    @Test
    fun `should schedule address created event via outbox`() {
        // given
        val event = anAddressCreatedEvent()
        every { outbox.schedule(any(), any<String>()) } returns Unit

        // when
        publisher.publish(event)

        // then
        verify { outbox.schedule(event, any<String>()) }
    }

    @Test
    fun `should use vaultId as partition key`() {
        // given
        val event = anAddressCreatedEvent()
        val keySlot = slot<String>()
        every { outbox.schedule(any(), capture(keySlot)) } returns Unit

        // when
        publisher.publish(event)

        // then
        assertThat(keySlot.captured).isEqualTo(event.vaultId.toString())
    }
}
