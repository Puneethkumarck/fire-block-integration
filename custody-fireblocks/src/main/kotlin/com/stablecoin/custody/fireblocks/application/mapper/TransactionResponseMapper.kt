package com.stablecoin.custody.fireblocks.application.mapper

import com.stablecoin.custody.fireblocks.api.response.EstimateFeeResponse
import com.stablecoin.custody.fireblocks.api.response.TransactionResponse
import com.stablecoin.custody.fireblocks.domain.port.FeeEstimateResult
import com.stablecoin.custody.fireblocks.domain.transaction.Transaction

fun Transaction.toResponse() =
    TransactionResponse(
        id = id.value,
        externalTxId = externalTxId,
        fireblocksTransactionId = fireblocksTransactionId,
        status = status.name,
        currency = currency,
        protocol = protocol,
        amount = amount,
        sourceVaultId = sourceVaultId,
        destinationAddress = destinationAddress,
        feeLevel = feeLevel.name,
        note = note,
        txHash = txHash,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

fun FeeEstimateResult.toResponse() =
    EstimateFeeResponse(
        low = low,
        medium = medium,
        high = high,
    )
