package com.stablecoin.custody.fireblocks.infrastructure.persistence

import com.stablecoin.custody.fireblocks.domain.wallet.SupportedAsset
import com.stablecoin.custody.fireblocks.domain.wallet.SupportedAssetRepository
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "supported_assets")
private class SupportedAssetEntity(
    @Id
    @Column(name = "id", nullable = false)
    val id: UUID,
    @Column(name = "currency", nullable = false)
    val currency: String,
    @Column(name = "protocol", nullable = false)
    val protocol: String,
    @Column(name = "fireblocks_asset_id", nullable = false, unique = true)
    val fireblocksAssetId: String,
    @Column(name = "taggable", nullable = false)
    val taggable: Boolean,
    @Column(name = "utxo", nullable = false)
    val utxo: Boolean,
    @Column(name = "active", nullable = false)
    val active: Boolean,
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant,
)

private interface SupportedAssetJpaRepository : JpaRepository<SupportedAssetEntity, UUID> {
    fun findByCurrencyAndProtocol(
        currency: String,
        protocol: String,
    ): SupportedAssetEntity?

    fun findByFireblocksAssetId(fireblocksAssetId: String): SupportedAssetEntity?

    fun findByActiveTrue(): List<SupportedAssetEntity>
}

@Component
private class SupportedAssetRepositoryAdapter(
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
