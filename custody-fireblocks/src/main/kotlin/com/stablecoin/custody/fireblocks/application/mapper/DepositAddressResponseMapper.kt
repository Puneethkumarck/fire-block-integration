package com.stablecoin.custody.fireblocks.application.mapper

import com.stablecoin.custody.fireblocks.api.response.DepositAddressResponse
import com.stablecoin.custody.fireblocks.domain.wallet.DepositAddress

fun DepositAddress.toResponse() =
    DepositAddressResponse(
        id = id.value,
        walletAssetId = walletAssetId.value,
        address = address,
        tag = tag,
        legacyAddress = legacyAddress,
        createdAt = createdAt,
    )
