package com.stablecoin.custody.fireblocks.infrastructure.messaging.outbox

import com.stablecoin.custody.fireblocks.domain.shared.logger
import io.namastack.outbox.Outbox
import kotlin.reflect.full.memberProperties

private val log = logger<AbstractOutboxEventPublisher>()

abstract class AbstractOutboxEventPublisher(
    private val outbox: Outbox,
    private val keyFieldNames: List<String>,
) {
    protected fun schedule(event: Any) {
        val key = resolveKey(event)
        outbox.schedule(event, key)
        log.debug("Scheduled outbox event type={} key={}", event::class.simpleName, key)
    }

    private fun resolveKey(event: Any): String {
        for (fieldName in keyFieldNames) {
            val prop = event::class.memberProperties.find { it.name == fieldName }
            if (prop != null) {
                val value = prop.getter.call(event)
                if (value != null) return value.toString()
            }
        }
        throw IllegalArgumentException(
            "Event class has no non-null value for any of $keyFieldNames: ${event::class.qualifiedName}",
        )
    }
}
