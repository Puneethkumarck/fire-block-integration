package com.stablecoin.custody.fireblocks.infrastructure.messaging.outbox

import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.namastack.outbox.Outbox
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class AbstractOutboxEventPublisherTest {
    private val outbox: Outbox = mockk()

    @Test
    fun `should throw when event has no matching key field`() {
        // given
        val publisher =
            object : AbstractOutboxEventPublisher(outbox, listOf("nonExistentField")) {
                fun publishAny(event: Any) = schedule(event)
            }
        val event =
            object {
                val someField = "value"
            }

        // when / then
        assertThatThrownBy { publisher.publishAny(event) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("no non-null value for any of [nonExistentField]")
    }
}
