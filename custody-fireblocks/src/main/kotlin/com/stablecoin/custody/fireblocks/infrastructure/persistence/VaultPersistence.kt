package com.stablecoin.custody.fireblocks.infrastructure.persistence

import com.stablecoin.custody.fireblocks.domain.vault.Vault
import com.stablecoin.custody.fireblocks.domain.vault.VaultId
import com.stablecoin.custody.fireblocks.domain.vault.VaultRepository
import com.stablecoin.custody.fireblocks.domain.vault.VaultStatus
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.Version
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "vaults")
private class VaultEntity(
    @Id
    @Column(name = "id", nullable = false)
    val id: UUID,
    @Column(name = "fireblocks_vault_id")
    var fireblocksVaultId: String? = null,
    @Column(name = "customer_ref_id", nullable = false, unique = true)
    val customerRefId: String,
    @Column(name = "name", nullable = false)
    val name: String,
    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    var status: VaultStatus,
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant,
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant,
    @Version
    var version: Long = 0,
)

private interface VaultJpaRepository : JpaRepository<VaultEntity, UUID> {
    fun findByCustomerRefId(customerRefId: String): VaultEntity?

    @Query("SELECT v FROM VaultEntity v WHERE v.status = :status AND v.createdAt < :cutoff")
    fun findPendingOlderThan(
        status: VaultStatus,
        cutoff: Instant,
    ): List<VaultEntity>
}

@Component
private class VaultRepositoryAdapter(
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
