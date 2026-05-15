package com.stablecoin.custody.fireblocks.infrastructure.persistence

import com.stablecoin.custody.fireblocks.domain.wallet.AssetStatus
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
@Table(name = "wallet_assets")
internal class WalletAssetEntity(
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
