package com.stablecoin.custody.fireblocks.domain.exception

class VaultNotActiveException(
    vaultId: String,
) : RuntimeException("Vault $vaultId is not active")
