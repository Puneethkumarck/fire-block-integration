package com.stablecoin.custody.fireblocks.infrastructure.persistence

import com.stablecoin.custody.fireblocks.domain.shared.Precision
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.Version
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "internal_balances")
internal class InternalBalanceEntity(
    @Id
    @Column(name = "id", nullable = false)
    val id: UUID,
    @Column(name = "vault_id", nullable = false)
    val vaultId: UUID,
    @Column(name = "currency", nullable = false)
    val currency: String,
    @Column(name = "protocol", nullable = false)
    val protocol: String,
    @Column(name = "balance", nullable = false, precision = Precision.PRECISION, scale = Precision.SCALE)
    var balance: BigDecimal,
    @Column(name = "last_transaction_id")
    var lastTransactionId: UUID? = null,
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant,
    @Version
    var version: Long = 0,
)
