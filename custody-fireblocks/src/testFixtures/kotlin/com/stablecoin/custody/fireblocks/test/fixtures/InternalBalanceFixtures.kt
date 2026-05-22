package com.stablecoin.custody.fireblocks.test.fixtures

import com.stablecoin.custody.fireblocks.domain.reconciliation.InternalBalance
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

fun anInternalBalance(
    id: UUID = UUID.randomUUID(),
    vaultId: UUID = UUID.randomUUID(),
    currency: String = "EURC",
    protocol: String = "ETH",
    balance: BigDecimal = BigDecimal("1000.00"),
    lastTransactionId: UUID? = null,
    updatedAt: Instant = Instant.now(),
    version: Long = 0,
) = InternalBalance(
    id = id,
    vaultId = vaultId,
    currency = currency,
    protocol = protocol,
    balance = balance,
    lastTransactionId = lastTransactionId,
    updatedAt = updatedAt,
    version = version,
)
