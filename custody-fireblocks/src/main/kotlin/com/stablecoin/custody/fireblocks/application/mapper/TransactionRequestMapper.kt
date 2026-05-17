package com.stablecoin.custody.fireblocks.application.mapper

import com.stablecoin.custody.fireblocks.api.request.EstimateFeeRequest
import com.stablecoin.custody.fireblocks.api.request.SubmitTransactionRequest
import com.stablecoin.custody.fireblocks.domain.port.FireblocksEstimateFeeCommand
import com.stablecoin.custody.fireblocks.domain.transaction.FeeLevel
import com.stablecoin.custody.fireblocks.domain.transaction.SubmitTransactionCommand

fun SubmitTransactionRequest.toCommand() =
    SubmitTransactionCommand(
        externalTxId = externalTxId,
        sourceVaultId = sourceVaultId,
        destinationAddress = destinationAddress,
        currency = currency,
        protocol = protocol,
        amount = amount,
        feeLevel = feeLevel?.let { FeeLevel.valueOf(it) },
        treatAsGrossAmount = treatAsGrossAmount,
        note = note,
    )

fun EstimateFeeRequest.toEstimateFeeCommand(
    fireblocksVaultId: String,
    fireblocksAssetId: String,
) = FireblocksEstimateFeeCommand(
    sourceVaultId = fireblocksVaultId,
    destinationAddress = destinationAddress,
    assetId = fireblocksAssetId,
    amount = amount,
)
