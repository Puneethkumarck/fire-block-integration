package com.stablecoin.custody.fireblocks.infrastructure.temporal.dto

import java.math.BigDecimal

data class FireblocksSubmitActivityCommand(
    val externalTxId: String,
    val sourceVaultId: String,
    val destinationAddress: String,
    val assetId: String,
    val amount: BigDecimal,
    val feeLevel: String,
    val treatAsGrossAmount: Boolean,
    val note: String?,
)

data class ReserveFundsActivityCommand(
    val vaultId: String,
    val assetId: String,
    val amount: BigDecimal,
)

data class ReleaseFundsActivityCommand(
    val allocationId: String,
)
