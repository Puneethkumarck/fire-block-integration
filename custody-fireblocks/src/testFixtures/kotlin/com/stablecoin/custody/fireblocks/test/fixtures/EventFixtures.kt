package com.stablecoin.custody.fireblocks.test.fixtures

import com.stablecoin.custody.fireblocks.domain.event.AddressCreatedEvent
import com.stablecoin.custody.fireblocks.domain.event.TransactionStatusChangedEvent
import com.stablecoin.custody.fireblocks.domain.event.VaultCreatedEvent
import com.stablecoin.custody.fireblocks.domain.event.WalletAssetCreatedEvent
import java.time.Instant
import java.util.UUID

fun aVaultCreatedEvent(
    vaultId: UUID = UUID.randomUUID(),
    customerRefId: String = "kyc-ref-${UUID.randomUUID()}",
    fireblocksVaultId: String = "fb-vault-123",
    occurredAt: Instant = Instant.now(),
) = VaultCreatedEvent(
    vaultId = vaultId,
    customerRefId = customerRefId,
    fireblocksVaultId = fireblocksVaultId,
    occurredAt = occurredAt,
)

fun aWalletAssetCreatedEvent(
    walletAssetId: UUID = UUID.randomUUID(),
    vaultId: UUID = UUID.randomUUID(),
    currency: String = "BTC",
    protocol: String = "BTC",
    fireblocksAssetId: String = "BTC",
    occurredAt: Instant = Instant.now(),
) = WalletAssetCreatedEvent(
    walletAssetId = walletAssetId,
    vaultId = vaultId,
    currency = currency,
    protocol = protocol,
    fireblocksAssetId = fireblocksAssetId,
    occurredAt = occurredAt,
)

fun anAddressCreatedEvent(
    depositAddressId: UUID = UUID.randomUUID(),
    walletAssetId: UUID = UUID.randomUUID(),
    vaultId: UUID = UUID.randomUUID(),
    address: String = "0x1234567890abcdef1234567890abcdef12345678",
    tag: String? = null,
    occurredAt: Instant = Instant.now(),
) = AddressCreatedEvent(
    depositAddressId = depositAddressId,
    walletAssetId = walletAssetId,
    vaultId = vaultId,
    address = address,
    tag = tag,
    occurredAt = occurredAt,
)

fun aTransactionStatusChangedEvent(
    transactionId: UUID = UUID.randomUUID(),
    externalTxId: String = "ext-tx-${UUID.randomUUID()}",
    previousStatus: String = "CREATED",
    newStatus: String = "PROCESSING",
    fireblocksStatus: String? = "SUBMITTED",
    subStatus: String? = null,
    txHash: String? = null,
    occurredAt: Instant = Instant.now(),
) = TransactionStatusChangedEvent(
    transactionId = transactionId,
    externalTxId = externalTxId,
    previousStatus = previousStatus,
    newStatus = newStatus,
    fireblocksStatus = fireblocksStatus,
    subStatus = subStatus,
    txHash = txHash,
    occurredAt = occurredAt,
)
