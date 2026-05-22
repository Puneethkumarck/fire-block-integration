package com.stablecoin.custody.fireblocks.domain.reconciliation

import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

data class InternalBalance(
    val id: UUID,
    val vaultId: UUID,
    val currency: String,
    val protocol: String,
    val balance: BigDecimal,
    val lastTransactionId: UUID?,
    val updatedAt: Instant,
    val version: Long = 0,
) {
    init {
        require(currency.isNotBlank()) { "currency must not be blank" }
        require(protocol.isNotBlank()) { "protocol must not be blank" }
    }

    fun debit(
        amount: BigDecimal,
        transactionId: UUID,
    ): InternalBalance {
        require(amount > BigDecimal.ZERO) { "debit amount must be positive" }
        return copy(
            balance = balance.subtract(amount),
            lastTransactionId = transactionId,
            updatedAt = Instant.now(),
        )
    }
}
