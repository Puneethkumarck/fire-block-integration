package com.stablecoin.custody.fireblocks.test.fixtures

import com.stablecoin.custody.fireblocks.domain.vault.VaultId
import com.stablecoin.custody.fireblocks.domain.wallet.ActivateAssetCommand
import com.stablecoin.custody.fireblocks.domain.wallet.AssetStatus
import com.stablecoin.custody.fireblocks.domain.wallet.DepositAddress
import com.stablecoin.custody.fireblocks.domain.wallet.DepositAddressId
import com.stablecoin.custody.fireblocks.domain.wallet.GenerateAddressCommand
import com.stablecoin.custody.fireblocks.domain.wallet.SupportedAsset
import com.stablecoin.custody.fireblocks.domain.wallet.WalletAsset
import com.stablecoin.custody.fireblocks.domain.wallet.WalletAssetId
import java.time.Instant
import java.util.UUID

fun aSupportedAsset(
    id: UUID = UUID.randomUUID(),
    currency: String = "BTC",
    protocol: String = "BTC",
    fireblocksAssetId: String = "BTC",
    taggable: Boolean = false,
    utxo: Boolean = true,
    active: Boolean = true,
    createdAt: Instant = Instant.now(),
) = SupportedAsset(
    id = id,
    currency = currency,
    protocol = protocol,
    fireblocksAssetId = fireblocksAssetId,
    taggable = taggable,
    utxo = utxo,
    active = active,
    createdAt = createdAt,
)

fun aWalletAsset(
    id: WalletAssetId = WalletAssetId(UUID.randomUUID()),
    vaultId: VaultId = VaultId(UUID.randomUUID()),
    currency: String = "BTC",
    protocol: String = "BTC",
    fireblocksAssetId: String = "BTC",
    status: AssetStatus = AssetStatus.ACTIVE,
    createdAt: Instant = Instant.now(),
    updatedAt: Instant = Instant.now(),
    version: Long = 0,
) = WalletAsset(
    id = id,
    vaultId = vaultId,
    currency = currency,
    protocol = protocol,
    fireblocksAssetId = fireblocksAssetId,
    status = status,
    createdAt = createdAt,
    updatedAt = updatedAt,
    version = version,
)

fun aDepositAddress(
    id: DepositAddressId = DepositAddressId(UUID.randomUUID()),
    walletAssetId: WalletAssetId = WalletAssetId(UUID.randomUUID()),
    address: String = "0x1234567890abcdef1234567890abcdef12345678",
    tag: String? = null,
    legacyAddress: String? = null,
    createdAt: Instant = Instant.now(),
    updatedAt: Instant = Instant.now(),
    version: Long = 0,
) = DepositAddress(
    id = id,
    walletAssetId = walletAssetId,
    address = address,
    tag = tag,
    legacyAddress = legacyAddress,
    createdAt = createdAt,
    updatedAt = updatedAt,
    version = version,
)

fun anActivateAssetCommand(
    vaultId: VaultId = VaultId(UUID.randomUUID()),
    currency: String = "BTC",
    protocol: String = "BTC",
) = ActivateAssetCommand(
    vaultId = vaultId,
    currency = currency,
    protocol = protocol,
)

fun aGenerateAddressCommand(
    vaultId: VaultId = VaultId(UUID.randomUUID()),
    currency: String = "BTC",
    protocol: String = "BTC",
) = GenerateAddressCommand(
    vaultId = vaultId,
    currency = currency,
    protocol = protocol,
)
