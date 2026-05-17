package com.stablecoin.custody.fireblocks.application.mapper

import com.stablecoin.custody.fireblocks.api.response.VaultResponse
import com.stablecoin.custody.fireblocks.domain.vault.Vault

fun Vault.toResponse() =
    VaultResponse(
        id = id.value,
        fireblocksVaultId = fireblocksVaultId,
        customerRefId = customerRefId,
        name = name,
        status = status.name,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
