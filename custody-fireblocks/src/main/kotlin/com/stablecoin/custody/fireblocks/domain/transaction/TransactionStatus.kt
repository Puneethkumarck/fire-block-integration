package com.stablecoin.custody.fireblocks.domain.transaction

enum class TransactionStatus(
    val terminal: Boolean,
) {
    CREATED(false),
    SUBMITTED(false),
    PROCESSING(false),
    CONFIRMING(false),
    CONFIRMED(true),
    FAILED(true),
    ;

    companion object {
        fun fromFireblocksStatus(fireblocksStatus: String): TransactionStatus =
            when (fireblocksStatus) {
                "SUBMITTED", "PENDING_SIGNATURE", "PENDING_AUTHORIZATION",
                "QUEUED", "BROADCASTING", "PENDING_AML_SCREENING", "CANCELLING",
                -> PROCESSING
                "CONFIRMING" -> CONFIRMING
                "COMPLETED" -> CONFIRMED
                "PARTIALLY_COMPLETED", "FAILED", "REJECTED", "CANCELLED",
                "BLOCKED", "TIMEOUT",
                -> FAILED
                else -> throw IllegalArgumentException("Unknown Fireblocks status: $fireblocksStatus")
            }
    }
}
