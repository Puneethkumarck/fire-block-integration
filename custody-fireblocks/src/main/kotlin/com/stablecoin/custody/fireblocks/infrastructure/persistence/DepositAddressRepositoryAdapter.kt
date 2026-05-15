package com.stablecoin.custody.fireblocks.infrastructure.persistence

import com.stablecoin.custody.fireblocks.domain.wallet.DepositAddress
import com.stablecoin.custody.fireblocks.domain.wallet.DepositAddressId
import com.stablecoin.custody.fireblocks.domain.wallet.DepositAddressRepository
import com.stablecoin.custody.fireblocks.domain.wallet.WalletAssetId
import org.springframework.stereotype.Component

@Component
internal class DepositAddressRepositoryAdapter(
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
