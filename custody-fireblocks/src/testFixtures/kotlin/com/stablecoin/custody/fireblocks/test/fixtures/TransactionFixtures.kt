package com.stablecoin.custody.fireblocks.test.fixtures

import com.stablecoin.custody.fireblocks.domain.transaction.FeeLevel
import com.stablecoin.custody.fireblocks.domain.transaction.SubmitTransactionCommand
import com.stablecoin.custody.fireblocks.domain.transaction.Transaction
import com.stablecoin.custody.fireblocks.domain.transaction.TransactionId
import com.stablecoin.custody.fireblocks.domain.transaction.TransactionStatus
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

fun aTransaction(
    id: TransactionId = TransactionId(UUID.randomUUID()),
    externalTxId: String = "ext-tx-${UUID.randomUUID()}",
    fireblocksTransactionId: String? = null,
    status: TransactionStatus = TransactionStatus.CREATED,
    fireblocksStatus: String? = null,
    fireblocksSubStatus: String? = null,
    assetId: String = "BTC",
    currency: String = "BTC",
    protocol: String = "BTC",
    amount: BigDecimal = BigDecimal("1.5"),
    sourceVaultId: String = UUID.randomUUID().toString(),
    destinationAddress: String = "0x1234567890abcdef1234567890abcdef12345678",
    feeLevel: FeeLevel = FeeLevel.MEDIUM,
    treatAsGrossAmount: Boolean = false,
    txHash: String? = null,
    note: String? = null,
    createdAt: Instant = Instant.now(),
    updatedAt: Instant = Instant.now(),
    version: Long = 0,
) = Transaction(
    id = id,
    externalTxId = externalTxId,
    fireblocksTransactionId = fireblocksTransactionId,
    status = status,
    fireblocksStatus = fireblocksStatus,
    fireblocksSubStatus = fireblocksSubStatus,
    assetId = assetId,
    currency = currency,
    protocol = protocol,
    amount = amount,
    sourceVaultId = sourceVaultId,
    destinationAddress = destinationAddress,
    feeLevel = feeLevel,
    treatAsGrossAmount = treatAsGrossAmount,
    txHash = txHash,
    note = note,
    createdAt = createdAt,
    updatedAt = updatedAt,
    version = version,
)

fun aSubmitTransactionCommand(
    externalTxId: String = "ext-tx-${UUID.randomUUID()}",
    sourceVaultId: String = UUID.randomUUID().toString(),
    destinationAddress: String = "0x1234567890abcdef1234567890abcdef12345678",
    currency: String = "BTC",
    protocol: String = "BTC",
    amount: BigDecimal = BigDecimal("1.5"),
    feeLevel: FeeLevel? = null,
    treatAsGrossAmount: Boolean? = null,
    note: String? = null,
) = SubmitTransactionCommand(
    externalTxId = externalTxId,
    sourceVaultId = sourceVaultId,
    destinationAddress = destinationAddress,
    currency = currency,
    protocol = protocol,
    amount = amount,
    feeLevel = feeLevel,
    treatAsGrossAmount = treatAsGrossAmount,
    note = note,
)
