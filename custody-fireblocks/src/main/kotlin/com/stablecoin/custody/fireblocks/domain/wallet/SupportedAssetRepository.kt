package com.stablecoin.custody.fireblocks.domain.wallet

interface SupportedAssetRepository {
    fun findByCurrencyAndProtocol(
        currency: String,
        protocol: String,
    ): SupportedAsset?

    fun findByFireblocksAssetId(fireblocksAssetId: String): SupportedAsset?

    fun findAllActive(): List<SupportedAsset>
}
