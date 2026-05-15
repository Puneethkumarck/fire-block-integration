package com.stablecoin.custody.fireblocks.infrastructure.persistence

import com.stablecoin.custody.fireblocks.domain.vault.VaultId
import com.stablecoin.custody.fireblocks.domain.wallet.AssetStatus
import com.stablecoin.custody.fireblocks.domain.wallet.WalletAsset
import com.stablecoin.custody.fireblocks.domain.wallet.WalletAssetId
import com.stablecoin.custody.fireblocks.domain.wallet.WalletAssetRepository
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.Version
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "wallet_assets")
private class WalletAssetEntity(
    @Id
    @Column(name = "id", nullable = false)
    val id: UUID,
    @Column(name = "vault_id", nullable = false)
    val vaultId: UUID,
    @Column(name = "currency", nullable = false)
    val currency: String,
    @Column(name = "protocol", nullable = false)
    val protocol: String,
    @Column(name = "fireblocks_asset_id", nullable = false)
    val fireblocksAssetId: String,
    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    val status: AssetStatus,
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant,
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant,
    @Version
    var version: Long = 0,
)

private interface WalletAssetJpaRepository : JpaRepository<WalletAssetEntity, UUID> {
    fun findByVaultIdAndCurrencyAndProtocol(
        vaultId: UUID,
        currency: String,
        protocol: String,
    ): WalletAssetEntity?
}

@Component
private class WalletAssetRepositoryAdapter(
    private val jpaRepository: WalletAssetJpaRepository,
) : WalletAssetRepository {
    override fun findById(id: WalletAssetId): WalletAsset? =
        jpaRepository
            .findById(id.value)
            .map { it.toDomain() }
            .orElse(null)

    override fun findByVaultIdAndCurrencyAndProtocol(
        vaultId: VaultId,
        currency: String,
        protocol: String,
    ): WalletAsset? = jpaRepository.findByVaultIdAndCurrencyAndProtocol(vaultId.value, currency, protocol)?.toDomain()

    override fun save(walletAsset: WalletAsset): WalletAsset = walletAsset.toEntity().let { jpaRepository.saveAndFlush(it) }.toDomain()

    fun WalletAsset.toEntity() =
        WalletAssetEntity(
            id = id.value,
            vaultId = vaultId.value,
            currency = currency,
            protocol = protocol,
            fireblocksAssetId = fireblocksAssetId,
            status = status,
            createdAt = createdAt,
            updatedAt = updatedAt,
            version = version,
        )

    fun WalletAssetEntity.toDomain() =
        WalletAsset(
            id = WalletAssetId(id),
            vaultId = VaultId(vaultId),
            currency = currency,
            protocol = protocol,
            fireblocksAssetId = fireblocksAssetId,
            status = status,
            createdAt = createdAt,
            updatedAt = updatedAt,
            version = version,
        )
}
