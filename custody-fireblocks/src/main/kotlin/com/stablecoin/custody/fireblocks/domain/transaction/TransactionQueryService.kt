package com.stablecoin.custody.fireblocks.domain.transaction

import com.stablecoin.custody.fireblocks.domain.exception.TransactionNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class TransactionQueryService(
    private val transactionRepository: TransactionRepository,
) {
    @Transactional(readOnly = true)
    fun getByExternalTxId(externalTxId: String): Transaction =
        transactionRepository.findByExternalTxId(externalTxId)
            ?: throw TransactionNotFoundException(externalTxId)

    @Transactional(readOnly = true)
    fun getByFireblocksTxId(fireblocksTxId: String): Transaction =
        transactionRepository.findByFireblocksTransactionId(fireblocksTxId)
            ?: throw TransactionNotFoundException(fireblocksTxId)
}
