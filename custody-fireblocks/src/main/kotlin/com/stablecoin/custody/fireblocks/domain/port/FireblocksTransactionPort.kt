package com.stablecoin.custody.fireblocks.domain.port

interface FireblocksTransactionPort {
    fun submitTransaction(command: FireblocksSubmitCommand): TransactionResult

    fun getTransaction(fireblocksTxId: String): TransactionResult

    fun getByExternalId(externalTxId: String): TransactionResult?

    fun estimateFee(command: FireblocksEstimateFeeCommand): FeeEstimateResult

    fun cancelTransaction(fireblocksTxId: String): Boolean
}
