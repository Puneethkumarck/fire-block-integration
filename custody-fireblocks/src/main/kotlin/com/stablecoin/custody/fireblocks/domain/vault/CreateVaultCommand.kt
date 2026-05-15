package com.stablecoin.custody.fireblocks.domain.vault

data class CreateVaultCommand(
    val customerRefId: String,
    val name: String,
)
