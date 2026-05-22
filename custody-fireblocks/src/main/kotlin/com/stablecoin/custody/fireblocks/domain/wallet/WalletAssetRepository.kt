package com.stablecoin.custody.fireblocks.domain.wallet

import com.stablecoin.custody.fireblocks.domain.vault.VaultId

interface WalletAssetRepository {
    fun findById(id: WalletAssetId): WalletAsset?

    fun findByVaultIdAndCurrencyAndProtocol(
        vaultId: VaultId,
        currency: String,
        protocol: String,
    ): WalletAsset?

    fun save(walletAsset: WalletAsset): WalletAsset

    fun findByVaultId(vaultId: VaultId): List<WalletAsset>
}
