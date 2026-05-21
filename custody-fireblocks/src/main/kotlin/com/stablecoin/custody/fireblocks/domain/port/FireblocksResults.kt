package com.stablecoin.custody.fireblocks.domain.port

import java.math.BigDecimal

data class VaultResult(
    val id: String,
    val name: String,
)

data class WalletAssetResult(
    val id: String,
    val available: String?,
)

data class DepositAddressResult(
    val address: String,
    val tag: String?,
    val legacyAddress: String?,
)

data class TransactionResult(
    val id: String,
    val status: String,
    val subStatus: String?,
    val txHash: String?,
)

data class BalanceResult(
    val total: BigDecimal,
    val available: BigDecimal,
    val pending: BigDecimal,
    val frozen: BigDecimal,
    val locked: BigDecimal,
)

data class FeeEstimateResult(
    val low: BigDecimal,
    val medium: BigDecimal,
    val high: BigDecimal,
)

data class AllocationResult(
    val id: String,
    val status: String,
)
