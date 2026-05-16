package com.stablecoin.custody.fireblocks.api.test.fixtures

import com.stablecoin.custody.fireblocks.api.error.ApiError
import com.stablecoin.custody.fireblocks.api.response.BalanceResponse
import com.stablecoin.custody.fireblocks.api.response.DepositAddressResponse
import com.stablecoin.custody.fireblocks.api.response.EstimateFeeResponse
import com.stablecoin.custody.fireblocks.api.response.TransactionResponse
import com.stablecoin.custody.fireblocks.api.response.VaultResponse
import com.stablecoin.custody.fireblocks.api.response.WalletAssetResponse
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

fun aVaultResponse(
    id: UUID = UUID.randomUUID(),
    fireblocksVaultId: String? = "fb-vault-123",
    customerRefId: String = "customer-ref-001",
    name: String = "Test Vault",
    status: String = "ACTIVE",
    createdAt: Instant = Instant.now(),
    updatedAt: Instant = Instant.now(),
) = VaultResponse(
    id = id,
    fireblocksVaultId = fireblocksVaultId,
    customerRefId = customerRefId,
    name = name,
    status = status,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun aWalletAssetResponse(
    id: UUID = UUID.randomUUID(),
    vaultId: UUID = UUID.randomUUID(),
    currency: String = "EURC",
    protocol: String = "ETH",
    fireblocksAssetId: String = "EURC_ETH",
    status: String = "ACTIVE",
    createdAt: Instant = Instant.now(),
    updatedAt: Instant = Instant.now(),
) = WalletAssetResponse(
    id = id,
    vaultId = vaultId,
    currency = currency,
    protocol = protocol,
    fireblocksAssetId = fireblocksAssetId,
    status = status,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun aDepositAddressResponse(
    id: UUID = UUID.randomUUID(),
    walletAssetId: UUID = UUID.randomUUID(),
    address: String = "0x1234567890abcdef1234567890abcdef12345678",
    tag: String? = null,
    legacyAddress: String? = null,
    createdAt: Instant = Instant.now(),
) = DepositAddressResponse(
    id = id,
    walletAssetId = walletAssetId,
    address = address,
    tag = tag,
    legacyAddress = legacyAddress,
    createdAt = createdAt,
)

fun aTransactionResponse(
    id: UUID = UUID.randomUUID(),
    externalTxId: String = "ext-tx-001",
    fireblocksTransactionId: String? = "fb-tx-001",
    status: String = "CREATED",
    currency: String = "EURC",
    protocol: String = "ETH",
    amount: BigDecimal = BigDecimal("100.00"),
    sourceVaultId: String = "vault-001",
    destinationAddress: String = "0x1234567890abcdef1234567890abcdef12345678",
    feeLevel: String = "MEDIUM",
    note: String? = null,
    txHash: String? = null,
    createdAt: Instant = Instant.now(),
    updatedAt: Instant = Instant.now(),
) = TransactionResponse(
    id = id,
    externalTxId = externalTxId,
    fireblocksTransactionId = fireblocksTransactionId,
    status = status,
    currency = currency,
    protocol = protocol,
    amount = amount,
    sourceVaultId = sourceVaultId,
    destinationAddress = destinationAddress,
    feeLevel = feeLevel,
    note = note,
    txHash = txHash,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun aBalanceResponse(
    total: BigDecimal = BigDecimal("1000.00"),
    available: BigDecimal = BigDecimal("800.00"),
    pending: BigDecimal = BigDecimal("100.00"),
    frozen: BigDecimal = BigDecimal("50.00"),
    locked: BigDecimal = BigDecimal("50.00"),
) = BalanceResponse(
    total = total,
    available = available,
    pending = pending,
    frozen = frozen,
    locked = locked,
)

fun anEstimateFeeResponse(
    low: BigDecimal = BigDecimal("0.001"),
    medium: BigDecimal = BigDecimal("0.005"),
    high: BigDecimal = BigDecimal("0.01"),
) = EstimateFeeResponse(
    low = low,
    medium = medium,
    high = high,
)

fun anApiError(
    code: String = "CUSTODY-1001",
    status: String = "Not Found",
    message: String = "Resource not found",
    traceId: String? = "trace-001",
    details: Map<String, String?>? = null,
) = ApiError(
    code = code,
    status = status,
    message = message,
    traceId = traceId,
    details = details,
)
