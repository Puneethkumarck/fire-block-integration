package com.stablecoin.custody.fireblocks.infrastructure.messaging.outbox

import com.stablecoin.custody.fireblocks.domain.shared.logger
import io.namastack.outbox.handler.OutboxHandler
import io.namastack.outbox.handler.OutboxRecordMetadata
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

private val log = logger<ServiceOutboxHandler>()

@Component
class ServiceOutboxHandler(
    private val kafkaTemplate: KafkaTemplate<String, Any>,
) : OutboxHandler {
    override fun handle(
        payload: Any,
        metadata: OutboxRecordMetadata,
    ) {
        val topic = resolveStaticField(payload, "TOPIC")
        val key = metadata.key
        try {
            kafkaTemplate.send(topic, key, payload).get(10, TimeUnit.SECONDS)
            log.debug("Published outbox event type={} topic={} key={}", payload::class.simpleName, topic, key)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            throw RuntimeException("Kafka send interrupted for event ${payload::class.simpleName}", e)
        } catch (e: Exception) {
            log.error("Failed to publish event type={} topic={}: {}", payload::class.simpleName, topic, e.message)
            throw RuntimeException("Kafka send failed for event ${payload::class.simpleName}", e)
        }
    }

    private fun resolveStaticField(
        event: Any,
        fieldName: String,
    ): String =
        try {
            event.javaClass.getField(fieldName).get(null) as String
        } catch (e: NoSuchFieldException) {
            throw IllegalArgumentException("Event class missing static $fieldName field: ${event.javaClass.name}", e)
        } catch (e: IllegalAccessException) {
            throw IllegalArgumentException("Event class missing static $fieldName field: ${event.javaClass.name}", e)
        }
}
