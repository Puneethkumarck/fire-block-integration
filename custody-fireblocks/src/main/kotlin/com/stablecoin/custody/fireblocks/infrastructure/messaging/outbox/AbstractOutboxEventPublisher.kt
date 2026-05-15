package com.stablecoin.custody.fireblocks.infrastructure.messaging.outbox

import com.stablecoin.custody.fireblocks.domain.shared.logger
import io.namastack.outbox.Outbox
import java.util.Optional
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties

private val log = logger<AbstractOutboxEventPublisher>()

abstract class AbstractOutboxEventPublisher(
    private val outbox: Outbox,
    private val keyFieldNames: List<String>,
) {
    private val propertyCache = ConcurrentHashMap<Pair<Class<*>, String>, Optional<KProperty1<out Any, *>>>()

    protected fun schedule(event: Any) {
        val key = resolveKey(event)
        outbox.schedule(event, key)
        log.debug("Scheduled outbox event type={} key={}", event::class.simpleName, key)
    }

    private fun resolveKey(event: Any): String {
        for (fieldName in keyFieldNames) {
            val prop = getCachedProperty(event.javaClass, fieldName)
            if (prop != null) {
                val value = prop.getter.call(event)
                if (value != null) return value.toString()
            }
        }
        throw IllegalArgumentException(
            "Event class has no non-null value for any of $keyFieldNames: ${event::class.qualifiedName}",
        )
    }

    private fun getCachedProperty(
        clazz: Class<*>,
        fieldName: String,
    ): KProperty1<out Any, *>? =
        propertyCache
            .getOrPut(clazz to fieldName) {
                Optional.ofNullable(clazz.kotlin.memberProperties.find { it.name == fieldName })
            }.orElse(null)
}
