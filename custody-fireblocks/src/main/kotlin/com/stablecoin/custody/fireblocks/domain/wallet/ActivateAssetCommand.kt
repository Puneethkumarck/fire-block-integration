package com.stablecoin.custody.fireblocks.domain.wallet

import com.stablecoin.custody.fireblocks.domain.vault.VaultId

data class ActivateAssetCommand(
    val vaultId: VaultId,
    val currency: String,
    val protocol: String,
) {
    init {
        require(currency.isNotBlank()) { "currency must not be blank" }
        require(protocol.isNotBlank()) { "protocol must not be blank" }
    }
}
