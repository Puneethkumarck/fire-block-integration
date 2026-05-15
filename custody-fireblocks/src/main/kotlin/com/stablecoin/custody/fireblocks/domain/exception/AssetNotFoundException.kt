package com.stablecoin.custody.fireblocks.domain.exception

import com.stablecoin.custody.fireblocks.domain.shared.CustodyException
import com.stablecoin.custody.fireblocks.domain.shared.ErrorCode

class AssetNotFoundException(
    vaultId: String,
    assetId: String,
) : CustodyException(ErrorCode.ASSET_NOT_FOUND, "Asset $assetId not found in vault $vaultId")
