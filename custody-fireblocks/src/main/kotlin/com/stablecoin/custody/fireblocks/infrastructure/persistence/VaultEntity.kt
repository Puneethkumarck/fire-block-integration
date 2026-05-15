package com.stablecoin.custody.fireblocks.infrastructure.persistence

import com.stablecoin.custody.fireblocks.domain.vault.VaultStatus
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.Version
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "vaults")
internal class VaultEntity(
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
