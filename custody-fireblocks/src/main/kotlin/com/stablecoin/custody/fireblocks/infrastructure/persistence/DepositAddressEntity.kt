package com.stablecoin.custody.fireblocks.infrastructure.persistence

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.Version
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "deposit_addresses")
internal class DepositAddressEntity(
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
