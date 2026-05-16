package com.stablecoin.custody.fireblocks.application.stream

import com.stablecoin.custody.fireblocks.domain.transaction.TransactionStatusUpdateHandler
import com.stablecoin.custody.fireblocks.test.fixtures.aTransactionStatusChangedEvent
import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class TransactionStatusEventConsumerTest {
    private val handler: TransactionStatusUpdateHandler = mockk()
    private val consumer = TransactionStatusEventConsumer(handler)

    @Test
    fun `should delegate to handler`() {
        // given
        val event = aTransactionStatusChangedEvent()
        every { handler.handle(event) } just runs

        // when
        consumer.consume(event)

        // then
        verify { handler.handle(event) }
    }
}
