package com.stablecoin.custody.fireblocks.api.test.fixtures

import com.stablecoin.custody.fireblocks.api.request.ActivateAssetRequest
import com.stablecoin.custody.fireblocks.api.request.CreateVaultRequest
import com.stablecoin.custody.fireblocks.api.request.EstimateFeeRequest
import com.stablecoin.custody.fireblocks.api.request.SubmitTransactionRequest
import java.math.BigDecimal

fun aCreateVaultRequest(
    customerRefId: String = "customer-ref-001",
    name: String = "Test Vault",
) = CreateVaultRequest(
    customerRefId = customerRefId,
    name = name,
)

fun anActivateAssetRequest(
    currency: String = "EURC",
    protocol: String = "ETH",
) = ActivateAssetRequest(
    currency = currency,
    protocol = protocol,
)

fun aSubmitTransactionRequest(
    externalTxId: String = "ext-tx-001",
    sourceVaultId: String = "vault-001",
    destinationAddress: String = "0x1234567890abcdef1234567890abcdef12345678",
    currency: String = "EURC",
    protocol: String = "ETH",
    amount: BigDecimal = BigDecimal("100.00"),
    feeLevel: String? = null,
    treatAsGrossAmount: Boolean? = null,
    note: String? = null,
) = SubmitTransactionRequest(
    externalTxId = externalTxId,
    sourceVaultId = sourceVaultId,
    destinationAddress = destinationAddress,
    currency = currency,
    protocol = protocol,
    amount = amount,
    feeLevel = feeLevel,
    treatAsGrossAmount = treatAsGrossAmount,
    note = note,
)

fun anEstimateFeeRequest(
    sourceVaultId: String = "vault-001",
    destinationAddress: String = "0x1234567890abcdef1234567890abcdef12345678",
    currency: String = "EURC",
    protocol: String = "ETH",
    amount: BigDecimal = BigDecimal("100.00"),
) = EstimateFeeRequest(
    sourceVaultId = sourceVaultId,
    destinationAddress = destinationAddress,
    currency = currency,
    protocol = protocol,
    amount = amount,
)
