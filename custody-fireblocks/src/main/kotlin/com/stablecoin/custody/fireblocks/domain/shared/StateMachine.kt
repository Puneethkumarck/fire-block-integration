package com.stablecoin.custody.fireblocks.domain.shared

class StateMachine<S : Enum<S>, T : StateProvider<S>>(
    private val transitions: Map<S, Set<S>>,
    private val onInvalidTransition: (T, S) -> Nothing,
) {
    fun transition(
        entity: T,
        targetState: S,
    ): S {
        val allowedTargets = transitions[entity.currentState()]
        if (allowedTargets == null || targetState !in allowedTargets) {
            onInvalidTransition(entity, targetState)
        }
        return targetState
    }
}
