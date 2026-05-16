package com.stablecoin.custody.fireblocks.domain.wallet

import com.stablecoin.custody.fireblocks.domain.vault.VaultId

data class GenerateAddressCommand(
    val vaultId: VaultId,
    val currency: String,
    val protocol: String,
)
