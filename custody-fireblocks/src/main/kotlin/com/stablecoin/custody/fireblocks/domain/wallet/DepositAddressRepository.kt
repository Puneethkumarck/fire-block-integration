package com.stablecoin.custody.fireblocks.domain.wallet

interface DepositAddressRepository {
    fun findByWalletAssetId(walletAssetId: WalletAssetId): DepositAddress?

    fun save(depositAddress: DepositAddress): DepositAddress
}
