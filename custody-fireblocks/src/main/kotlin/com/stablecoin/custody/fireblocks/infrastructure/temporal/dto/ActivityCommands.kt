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
) {
    init {
        require(externalTxId.isNotBlank()) { "externalTxId must not be blank" }
        require(sourceVaultId.isNotBlank()) { "sourceVaultId must not be blank" }
        require(destinationAddress.isNotBlank()) { "destinationAddress must not be blank" }
        require(assetId.isNotBlank()) { "assetId must not be blank" }
        require(amount > BigDecimal.ZERO) { "amount must be positive" }
        require(feeLevel.isNotBlank()) { "feeLevel must not be blank" }
    }
}

data class ReserveFundsActivityCommand(
    val vaultId: String,
    val assetId: String,
    val amount: BigDecimal,
) {
    init {
        require(vaultId.isNotBlank()) { "vaultId must not be blank" }
        require(assetId.isNotBlank()) { "assetId must not be blank" }
        require(amount > BigDecimal.ZERO) { "amount must be positive" }
    }
}

data class ReleaseFundsActivityCommand(
    val allocationId: String,
) {
    init {
        require(allocationId.isNotBlank()) { "allocationId must not be blank" }
    }
}
