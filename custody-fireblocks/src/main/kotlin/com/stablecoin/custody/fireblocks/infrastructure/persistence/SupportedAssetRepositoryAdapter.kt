package com.stablecoin.custody.fireblocks.infrastructure.persistence

import com.stablecoin.custody.fireblocks.domain.wallet.SupportedAsset
import com.stablecoin.custody.fireblocks.domain.wallet.SupportedAssetRepository
import org.springframework.stereotype.Component

@Component
internal class SupportedAssetRepositoryAdapter(
    private val jpaRepository: SupportedAssetJpaRepository,
) : SupportedAssetRepository {
    override fun findByCurrencyAndProtocol(
        currency: String,
        protocol: String,
    ): SupportedAsset? = jpaRepository.findByCurrencyAndProtocol(currency, protocol)?.toDomain()

    override fun findByFireblocksAssetId(fireblocksAssetId: String): SupportedAsset? =
        jpaRepository.findByFireblocksAssetId(fireblocksAssetId)?.toDomain()

    override fun findAllActive(): List<SupportedAsset> = jpaRepository.findByActiveTrue().map { it.toDomain() }

    fun SupportedAssetEntity.toDomain() =
        SupportedAsset(
            id = id,
            currency = currency,
            protocol = protocol,
            fireblocksAssetId = fireblocksAssetId,
            taggable = taggable,
            utxo = utxo,
            active = active,
            createdAt = createdAt,
        )
}
