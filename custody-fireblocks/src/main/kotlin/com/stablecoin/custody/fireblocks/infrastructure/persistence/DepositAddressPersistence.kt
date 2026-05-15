package com.stablecoin.custody.fireblocks.infrastructure.persistence

import com.stablecoin.custody.fireblocks.domain.wallet.DepositAddress
import com.stablecoin.custody.fireblocks.domain.wallet.DepositAddressId
import com.stablecoin.custody.fireblocks.domain.wallet.DepositAddressRepository
import com.stablecoin.custody.fireblocks.domain.wallet.WalletAssetId
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.Version
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "deposit_addresses")
private class DepositAddressEntity(
    @Id
    @Column(name = "id", nullable = false)
    val id: UUID,
    @Column(name = "wallet_asset_id", nullable = false)
    val walletAssetId: UUID,
    @Column(name = "address", nullable = false)
    val address: String,
    @Column(name = "tag")
    val tag: String?,
    @Column(name = "legacy_address")
    val legacyAddress: String?,
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant,
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant,
    @Version
    var version: Long = 0,
)

private interface DepositAddressJpaRepository : JpaRepository<DepositAddressEntity, UUID> {
    fun findByWalletAssetId(walletAssetId: UUID): DepositAddressEntity?
}

@Component
private class DepositAddressRepositoryAdapter(
    private val jpaRepository: DepositAddressJpaRepository,
) : DepositAddressRepository {
    override fun findByWalletAssetId(walletAssetId: WalletAssetId): DepositAddress? =
        jpaRepository.findByWalletAssetId(walletAssetId.value)?.toDomain()

    override fun save(depositAddress: DepositAddress): DepositAddress =
        depositAddress.toEntity().let { jpaRepository.saveAndFlush(it) }.toDomain()

    fun DepositAddress.toEntity() =
        DepositAddressEntity(
            id = id.value,
            walletAssetId = walletAssetId.value,
            address = address,
            tag = tag,
            legacyAddress = legacyAddress,
            createdAt = createdAt,
            updatedAt = updatedAt,
            version = version,
        )

    fun DepositAddressEntity.toDomain() =
        DepositAddress(
            id = DepositAddressId(id),
            walletAssetId = WalletAssetId(walletAssetId),
            address = address,
            tag = tag,
            legacyAddress = legacyAddress,
            createdAt = createdAt,
            updatedAt = updatedAt,
            version = version,
        )
}
