package com.stablecoin.custody.fireblocks.infrastructure.messaging.outbox

import com.stablecoin.custody.fireblocks.domain.event.VaultCreatedEvent
import com.stablecoin.custody.fireblocks.domain.exception.OutboxPublishException
import com.stablecoin.custody.fireblocks.test.fixtures.aVaultCreatedEvent
import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.slot
import io.namastack.outbox.handler.OutboxRecordMetadata
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.SendResult
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException

@ExtendWith(MockKExtension::class)
class ServiceOutboxHandlerTest {
    private val kafkaTemplate: KafkaTemplate<String, Any> = mockk()
    private val handler = ServiceOutboxHandler(kafkaTemplate, 10L)

    @Test
    fun `should publish event to correct topic with key`() {
        // given
        val event = aVaultCreatedEvent()
        val metadata = mockk<OutboxRecordMetadata>()
        every { metadata.key } returns "test-key"
        val topicSlot = slot<String>()
        val keySlot = slot<String>()
        every { kafkaTemplate.send(capture(topicSlot), capture(keySlot), any()) } returns
            CompletableFuture.completedFuture(mockk<SendResult<String, Any>>())

        // when
        handler.handle(event, metadata)

        // then
        assertThat(topicSlot.captured).isEqualTo(VaultCreatedEvent.TOPIC)
        assertThat(keySlot.captured).isEqualTo("test-key")
    }

    @Test
    fun `should send event payload to kafka`() {
        // given
        val event = aVaultCreatedEvent()
        val metadata = mockk<OutboxRecordMetadata>()
        every { metadata.key } returns "test-key"
        val payloadSlot = slot<Any>()
        every { kafkaTemplate.send(any<String>(), any(), capture(payloadSlot)) } returns
            CompletableFuture.completedFuture(mockk<SendResult<String, Any>>())

        // when
        handler.handle(event, metadata)

        // then
        assertThat(payloadSlot.captured).isSameAs(event)
    }

    @Test
    fun `should throw when event class has no TOPIC field`() {
        // given
        val event = "not-a-domain-event"
        val metadata = mockk<OutboxRecordMetadata>()
        every { metadata.key } returns "test-key"

        // when / then
        assertThatThrownBy { handler.handle(event, metadata) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("missing static TOPIC field")
    }

    @Test
    fun `should throw OutboxPublishException on kafka send failure`() {
        // given
        val event = aVaultCreatedEvent()
        val metadata = mockk<OutboxRecordMetadata>()
        every { metadata.key } returns "test-key"
        val failedFuture = CompletableFuture<SendResult<String, Any>>()
        failedFuture.completeExceptionally(RuntimeException("Kafka broker unavailable"))
        every { kafkaTemplate.send(any<String>(), any(), any()) } returns failedFuture

        // when / then
        assertThatThrownBy { handler.handle(event, metadata) }
            .isInstanceOf(OutboxPublishException::class.java)
            .hasMessageContaining("Kafka send failed")
            .hasCauseInstanceOf(ExecutionException::class.java)
    }
}
