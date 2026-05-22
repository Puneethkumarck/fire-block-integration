package com.stablecoin.custody.fireblocks.domain.allocation

enum class AllocationStatus(
    val terminal: Boolean,
) {
    PENDING(false),
    LOCKED(false),
    CONSUMED(false),
    RELEASED(true),
    FAILED(true),
    ;

    companion object {
        private val allowedTransitions =
            mapOf(
                PENDING to setOf(LOCKED, FAILED),
                LOCKED to setOf(CONSUMED, RELEASED),
                CONSUMED to setOf(RELEASED),
            )

        fun canTransition(
            from: AllocationStatus,
            to: AllocationStatus,
        ): Boolean = allowedTransitions[from]?.contains(to) == true
    }
}
