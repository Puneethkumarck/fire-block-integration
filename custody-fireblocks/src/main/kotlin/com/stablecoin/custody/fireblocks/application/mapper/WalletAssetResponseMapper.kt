package com.stablecoin.custody.fireblocks.application.mapper

import com.stablecoin.custody.fireblocks.api.response.WalletAssetResponse
import com.stablecoin.custody.fireblocks.domain.wallet.WalletAsset

fun WalletAsset.toResponse() =
    WalletAssetResponse(
        id = id.value,
        vaultId = vaultId.value,
        currency = currency,
        protocol = protocol,
        fireblocksAssetId = fireblocksAssetId,
        status = status.name,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
