package com.stablecoin.custody.fireblocks.test.fixtures

import com.stablecoin.custody.fireblocks.domain.port.TransactionStatusSignal
import com.stablecoin.custody.fireblocks.infrastructure.temporal.dto.FireblocksSubmitActivityCommand
import com.stablecoin.custody.fireblocks.infrastructure.temporal.dto.StartTransactionRequest
import com.stablecoin.custody.fireblocks.infrastructure.temporal.dto.TemporalTransactionStatusSignal
import java.math.BigDecimal
import java.util.UUID

fun aStartTransactionRequest(
    externalTxId: String = "ext-tx-${UUID.randomUUID()}",
    fireblocksVaultId: String = "fireblocks-vault-456",
    fireblocksAssetId: String = "BTC",
    currency: String = "BTC",
    protocol: String = "BTC",
    amount: BigDecimal = BigDecimal("1.5"),
    destinationAddress: String = "0x1234567890abcdef1234567890abcdef12345678",
    feeLevel: String = "MEDIUM",
    treatAsGrossAmount: Boolean = false,
    note: String? = null,
) = StartTransactionRequest(
    externalTxId = externalTxId,
    fireblocksVaultId = fireblocksVaultId,
    fireblocksAssetId = fireblocksAssetId,
    currency = currency,
    protocol = protocol,
    amount = amount,
    destinationAddress = destinationAddress,
    feeLevel = feeLevel,
    treatAsGrossAmount = treatAsGrossAmount,
    note = note,
)

fun aTransactionStatusSignal(
    fireblocksTransactionId: String = "fb-tx-${UUID.randomUUID()}",
    fireblocksStatus: String = "COMPLETED",
    subStatus: String? = null,
    txHash: String? = null,
) = TransactionStatusSignal(
    fireblocksTransactionId = fireblocksTransactionId,
    fireblocksStatus = fireblocksStatus,
    subStatus = subStatus,
    txHash = txHash,
)

fun aTemporalTransactionStatusSignal(
    fireblocksTransactionId: String = "fb-tx-${UUID.randomUUID()}",
    fireblocksStatus: String = "COMPLETED",
    subStatus: String? = null,
    txHash: String? = null,
) = TemporalTransactionStatusSignal(
    fireblocksTransactionId = fireblocksTransactionId,
    fireblocksStatus = fireblocksStatus,
    subStatus = subStatus,
    txHash = txHash,
)

fun aFireblocksSubmitActivityCommand(
    externalTxId: String = "ext-tx-${UUID.randomUUID()}",
    sourceVaultId: String = "fireblocks-vault-456",
    destinationAddress: String = "0x1234567890abcdef1234567890abcdef12345678",
    assetId: String = "BTC",
    amount: BigDecimal = BigDecimal("1.5"),
    feeLevel: String = "MEDIUM",
    treatAsGrossAmount: Boolean = false,
    note: String? = null,
) = FireblocksSubmitActivityCommand(
    externalTxId = externalTxId,
    sourceVaultId = sourceVaultId,
    destinationAddress = destinationAddress,
    assetId = assetId,
    amount = amount,
    feeLevel = feeLevel,
    treatAsGrossAmount = treatAsGrossAmount,
    note = note,
)
