package com.stablecoin.custody.fireblocks.infrastructure.persistence

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "supported_assets")
internal class SupportedAssetEntity(
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
