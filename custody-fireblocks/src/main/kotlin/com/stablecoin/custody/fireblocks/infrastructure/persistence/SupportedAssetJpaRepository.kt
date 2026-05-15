package com.stablecoin.custody.fireblocks.infrastructure.persistence

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

internal interface SupportedAssetJpaRepository : JpaRepository<SupportedAssetEntity, UUID> {
    fun findByCurrencyAndProtocol(
        currency: String,
        protocol: String,
    ): SupportedAssetEntity?

    fun findByFireblocksAssetId(fireblocksAssetId: String): SupportedAssetEntity?

    fun findByActiveTrue(): List<SupportedAssetEntity>
}
