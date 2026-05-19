package com.stablecoin.custody.fireblocks.domain.port

data class TransactionStatusSignal(
    val fireblocksTransactionId: String,
    val fireblocksStatus: String,
    val subStatus: String?,
    val txHash: String?,
)

fun interface WorkflowSignalPort {
    fun signalTransactionStatus(
        externalTxId: String,
        signal: TransactionStatusSignal,
    )
}
