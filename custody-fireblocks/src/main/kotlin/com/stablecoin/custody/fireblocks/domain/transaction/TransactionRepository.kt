package com.stablecoin.custody.fireblocks.domain.transaction

import java.time.Instant

interface TransactionRepository {
    fun findById(id: TransactionId): Transaction?

    fun findByExternalTxId(externalTxId: String): Transaction?

    fun findByFireblocksTransactionId(fireblocksTxId: String): Transaction?

    fun save(transaction: Transaction): Transaction

    fun findByIdForUpdate(id: TransactionId): Transaction?

    fun findStaleNonTerminal(
        cutoff: Instant,
        limit: Int,
    ): List<Transaction>

    fun findStaleCreated(
        cutoff: Instant,
        limit: Int,
    ): List<Transaction>
}
