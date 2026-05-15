package com.stablecoin.custody.fireblocks.domain.transaction

import java.math.BigDecimal

data class SubmitTransactionCommand(
    val externalTxId: String,
    val sourceVaultId: String,
    val destinationAddress: String,
    val currency: String,
    val protocol: String,
    val amount: BigDecimal,
    val feeLevel: FeeLevel? = null,
    val treatAsGrossAmount: Boolean? = null,
    val note: String? = null,
)
