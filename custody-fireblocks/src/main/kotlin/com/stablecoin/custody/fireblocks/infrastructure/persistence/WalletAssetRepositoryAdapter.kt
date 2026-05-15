package com.stablecoin.custody.fireblocks.infrastructure.persistence

import com.stablecoin.custody.fireblocks.domain.vault.VaultId
import com.stablecoin.custody.fireblocks.domain.wallet.WalletAsset
import com.stablecoin.custody.fireblocks.domain.wallet.WalletAssetId
import com.stablecoin.custody.fireblocks.domain.wallet.WalletAssetRepository
import org.springframework.stereotype.Component

@Component
internal class WalletAssetRepositoryAdapter(
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
