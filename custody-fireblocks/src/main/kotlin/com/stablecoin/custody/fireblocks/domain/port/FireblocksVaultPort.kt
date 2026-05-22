package com.stablecoin.custody.fireblocks.domain.port

interface FireblocksVaultPort {
    fun createVault(
        name: String,
        customerRefId: String,
    ): VaultResult

    fun getVault(vaultAccountId: String): VaultResult

    fun createWalletAsset(
        vaultAccountId: String,
        assetId: String,
    ): WalletAssetResult

    fun generateDepositAddress(
        vaultAccountId: String,
        assetId: String,
    ): DepositAddressResult

    fun lockAllocation(command: LockAllocationCommand): AllocationResult

    fun releaseAllocation(command: ReleaseAllocationCommand)
}
