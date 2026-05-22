package com.stablecoin.custody.fireblocks.infrastructure.fireblocks.client.dto

import java.math.BigDecimal

data class CreateVaultAccountRequest(
    val name: String,
    val customerRefId: String,
    val autoFuel: Boolean = false,
)

data class FireblocksVaultAccountResponse(
    val id: String,
    val name: String,
    val customerRefId: String? = null,
)

data class FireblocksWalletAssetResponse(
    val id: String,
    val available: String? = null,
)

data class FireblocksDepositAddressResponse(
    val address: String,
    val tag: String? = null,
    val legacyAddress: String? = null,
)

data class GenerateAddressRequest(
    val description: String? = null,
    val customerRefId: String? = null,
)

data class FireblocksBalanceResponse(
    val id: String,
    val total: String,
    val available: String,
    val pending: String,
    val frozen: String,
    val locked: String,
)

data class CreateTransactionRequest(
    val externalTxId: String,
    val source: TransferPeerPath,
    val destination: DestinationTransferPeerPath,
    val assetId: String,
    val amount: String,
    val feeLevel: String? = null,
    val treatAsGrossAmount: Boolean? = null,
    val note: String? = null,
)

data class FireblocksTransactionResponse(
    val id: String,
    val status: String,
    val subStatus: String? = null,
    val txHash: String? = null,
)

data class FireblocksEstimateFeeRequest(
    val assetId: String,
    val source: TransferPeerPath,
    val destination: DestinationTransferPeerPath,
    val amount: String,
)

data class FireblocksEstimateFeeResponse(
    val low: FireblocksFeeLevel,
    val medium: FireblocksFeeLevel,
    val high: FireblocksFeeLevel,
)

data class FireblocksFeeLevel(
    val networkFee: String? = null,
    val gasPrice: String? = null,
    val feePerByte: String? = null,
)

data class TransferPeerPath(
    val type: String,
    val id: String,
)

data class DestinationTransferPeerPath(
    val type: String,
    val oneTimeAddress: OneTimeAddress? = null,
)

data class OneTimeAddress(
    val address: String,
    val tag: String? = null,
)

data class FireblocksCancelTransactionResponse(
    val success: Boolean,
)

data class LockAllocationRequest(
    val allocationId: String,
    val assetId: String,
    val amount: BigDecimal,
)

data class ReleaseAllocationRequest(
    val allocationId: String,
    val assetId: String,
)

data class LockAllocationResponse(
    val id: String,
    val status: String,
)
