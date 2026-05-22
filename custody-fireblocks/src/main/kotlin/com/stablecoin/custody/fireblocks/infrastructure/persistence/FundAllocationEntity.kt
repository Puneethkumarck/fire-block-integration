package com.stablecoin.custody.fireblocks.infrastructure.persistence

import com.stablecoin.custody.fireblocks.domain.allocation.AllocationStatus
import com.stablecoin.custody.fireblocks.domain.shared.Precision
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.Version
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "fund_allocations")
internal class FundAllocationEntity(
    @Id
    @Column(name = "id", nullable = false)
    val id: UUID,
    @Column(name = "allocation_id", nullable = false, unique = true)
    val allocationId: String,
    @Column(name = "vault_id", nullable = false)
    val vaultId: UUID,
    @Column(name = "fireblocks_vault_id", nullable = false)
    val fireblocksVaultId: String,
    @Column(name = "asset_id", nullable = false)
    val assetId: String,
    @Column(name = "currency", nullable = false)
    val currency: String,
    @Column(name = "protocol", nullable = false)
    val protocol: String,
    @Column(name = "amount", nullable = false, precision = Precision.PRECISION, scale = Precision.SCALE)
    val amount: BigDecimal,
    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    var status: AllocationStatus,
    @Column(name = "transaction_id")
    var transactionId: UUID? = null,
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant,
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant,
    @Version
    var version: Long = 0,
)
