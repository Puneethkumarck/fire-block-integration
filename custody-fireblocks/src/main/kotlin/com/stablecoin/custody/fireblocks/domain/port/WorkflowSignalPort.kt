package com.stablecoin.custody.fireblocks.domain.port

data class TransactionStatusSignal(
    val fireblocksTransactionId: String,
    val fireblocksStatus: String,
    val subStatus: String?,
    val txHash: String?,
) {
    init {
        require(fireblocksTransactionId.isNotBlank()) { "fireblocksTransactionId must not be blank" }
        require(fireblocksStatus.isNotBlank()) { "fireblocksStatus must not be blank" }
    }
}

fun interface WorkflowSignalPort {
    fun signalTransactionStatus(
        externalTxId: String,
        signal: TransactionStatusSignal,
    )
}
