package com.stablecoin.custody.fireblocks.domain.shared

fun interface StateProvider<S> {
    fun currentState(): S
}
