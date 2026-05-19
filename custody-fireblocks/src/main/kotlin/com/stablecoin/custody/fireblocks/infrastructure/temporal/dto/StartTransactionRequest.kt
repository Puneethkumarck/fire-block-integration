package com.stablecoin.custody.fireblocks.infrastructure.temporal.dto

import java.math.BigDecimal

data class StartTransactionRequest(
    val externalTxId: String,
    val fireblocksVaultId: String,
    val fireblocksAssetId: String,
    val currency: String,
    val protocol: String,
    val amount: BigDecimal,
    val destinationAddress: String,
    val feeLevel: String,
    val treatAsGrossAmount: Boolean,
    val note: String?,
)
