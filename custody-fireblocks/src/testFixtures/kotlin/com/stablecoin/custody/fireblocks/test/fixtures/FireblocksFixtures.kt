package com.stablecoin.custody.fireblocks.test.fixtures

import com.stablecoin.custody.fireblocks.domain.port.DepositAddressResult
import com.stablecoin.custody.fireblocks.domain.port.VaultResult
import com.stablecoin.custody.fireblocks.domain.port.WalletAssetResult

fun aVaultResult(
    id: String = "fireblocks-vault-456",
    name: String = "Test Vault",
) = VaultResult(id = id, name = name)

fun aWalletAssetResult(
    id: String = "BTC",
    available: String? = "0",
) = WalletAssetResult(id = id, available = available)

fun aDepositAddressResult(
    address: String = "0x1234567890abcdef1234567890abcdef12345678",
    tag: String? = null,
    legacyAddress: String? = null,
) = DepositAddressResult(address = address, tag = tag, legacyAddress = legacyAddress)
