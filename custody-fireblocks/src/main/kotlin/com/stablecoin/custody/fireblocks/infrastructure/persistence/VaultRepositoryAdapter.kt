package com.stablecoin.custody.fireblocks.infrastructure.persistence

import com.stablecoin.custody.fireblocks.domain.vault.Vault
import com.stablecoin.custody.fireblocks.domain.vault.VaultId
import com.stablecoin.custody.fireblocks.domain.vault.VaultRepository
import com.stablecoin.custody.fireblocks.domain.vault.VaultStatus
import org.springframework.stereotype.Component
import java.time.Instant

@Component
internal class VaultRepositoryAdapter(
    private val jpaRepository: VaultJpaRepository,
) : VaultRepository {
    override fun findById(id: VaultId): Vault? =
        jpaRepository
            .findById(id.value)
            .map { it.toDomain() }
            .orElse(null)

    override fun findByCustomerRefId(customerRefId: String): Vault? =
        jpaRepository
            .findByCustomerRefId(customerRefId)
            ?.toDomain()

    override fun save(vault: Vault): Vault = vault.toEntity().let { jpaRepository.saveAndFlush(it) }.toDomain()

    override fun findPendingOlderThan(cutoff: Instant): List<Vault> =
        jpaRepository.findPendingOlderThan(VaultStatus.PENDING, cutoff).map { it.toDomain() }

    fun Vault.toEntity() =
        VaultEntity(
            id = id.value,
            fireblocksVaultId = fireblocksVaultId,
            customerRefId = customerRefId,
            name = name,
            status = status,
            createdAt = createdAt,
            updatedAt = updatedAt,
            version = version,
        )

    fun VaultEntity.toDomain() =
        Vault(
            id = VaultId(id),
            fireblocksVaultId = fireblocksVaultId,
            customerRefId = customerRefId,
            name = name,
            status = status,
            createdAt = createdAt,
            updatedAt = updatedAt,
            version = version,
        )
}
