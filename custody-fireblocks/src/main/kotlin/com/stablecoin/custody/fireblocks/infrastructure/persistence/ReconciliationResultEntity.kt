package com.stablecoin.custody.fireblocks.infrastructure.persistence

import com.stablecoin.custody.fireblocks.domain.reconciliation.ReconciliationStatus
import com.stablecoin.custody.fireblocks.domain.shared.Precision
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "reconciliation_results")
internal class ReconciliationResultEntity(
    @Id
    @Column(name = "id", nullable = false)
    val id: UUID,
    @Column(name = "vault_id", nullable = false)
    val vaultId: UUID,
    @Column(name = "currency", nullable = false)
    val currency: String,
    @Column(name = "protocol", nullable = false)
    val protocol: String,
    @Column(name = "internal_balance", nullable = false, precision = Precision.PRECISION, scale = Precision.SCALE)
    val internalBalance: BigDecimal,
    @Column(name = "fireblocks_balance", nullable = false, precision = Precision.PRECISION, scale = Precision.SCALE)
    val fireblocksBalance: BigDecimal,
    @Column(name = "drift", nullable = false, precision = Precision.PRECISION, scale = Precision.SCALE)
    val drift: BigDecimal,
    @Column(name = "absolute_drift", nullable = false, precision = Precision.PRECISION, scale = Precision.SCALE)
    val absoluteDrift: BigDecimal,
    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    val status: ReconciliationStatus,
    @Column(name = "tolerance_used", nullable = false, precision = Precision.PRECISION, scale = Precision.SCALE)
    val toleranceUsed: BigDecimal,
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant,
)
