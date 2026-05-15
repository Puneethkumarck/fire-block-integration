package com.stablecoin.custody.fireblocks.domain.port

fun interface EventPublisher<T> {
    fun publish(event: T)
}
