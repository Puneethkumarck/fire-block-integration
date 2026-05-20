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
) {
    init {
        require(externalTxId.isNotBlank()) { "externalTxId must not be blank" }
        require(fireblocksVaultId.isNotBlank()) { "fireblocksVaultId must not be blank" }
        require(fireblocksAssetId.isNotBlank()) { "fireblocksAssetId must not be blank" }
        require(currency.isNotBlank()) { "currency must not be blank" }
        require(protocol.isNotBlank()) { "protocol must not be blank" }
        require(amount > BigDecimal.ZERO) { "amount must be positive" }
        require(destinationAddress.isNotBlank()) { "destinationAddress must not be blank" }
        require(feeLevel.isNotBlank()) { "feeLevel must not be blank" }
    }
}
