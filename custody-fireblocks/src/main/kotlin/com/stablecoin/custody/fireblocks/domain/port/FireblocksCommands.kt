package com.stablecoin.custody.fireblocks.domain.port

import com.stablecoin.custody.fireblocks.domain.transaction.FeeLevel
import java.math.BigDecimal

data class FireblocksSubmitCommand(
    val externalTxId: String,
    val sourceVaultId: String,
    val destinationAddress: String,
    val assetId: String,
    val amount: BigDecimal,
    val feeLevel: FeeLevel,
    val treatAsGrossAmount: Boolean,
    val note: String?,
)

data class FireblocksEstimateFeeCommand(
    val sourceVaultId: String,
    val destinationAddress: String,
    val assetId: String,
    val amount: BigDecimal,
)

data class LockAllocationCommand(
    val allocationId: String,
    val vaultAccountId: String,
    val assetId: String,
    val amount: BigDecimal,
) {
    init {
        require(allocationId.isNotBlank()) { "allocationId must not be blank" }
        require(vaultAccountId.isNotBlank()) { "vaultAccountId must not be blank" }
        require(assetId.isNotBlank()) { "assetId must not be blank" }
        require(amount > BigDecimal.ZERO) { "amount must be positive" }
    }
}

data class ReleaseAllocationCommand(
    val allocationId: String,
    val vaultAccountId: String,
    val assetId: String,
) {
    init {
        require(allocationId.isNotBlank()) { "allocationId must not be blank" }
        require(vaultAccountId.isNotBlank()) { "vaultAccountId must not be blank" }
        require(assetId.isNotBlank()) { "assetId must not be blank" }
    }
}
