package com.stablecoin.custody.fireblocks.infrastructure.persistence

import com.stablecoin.custody.fireblocks.domain.shared.Precision
import com.stablecoin.custody.fireblocks.domain.transaction.FeeLevel
import com.stablecoin.custody.fireblocks.domain.transaction.TransactionStatus
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
@Table(name = "transactions")
internal class TransactionEntity(
    @Id
    @Column(name = "id", nullable = false)
    val id: UUID,
    @Column(name = "external_tx_id", nullable = false, unique = true)
    val externalTxId: String,
    @Column(name = "fireblocks_transaction_id")
    var fireblocksTransactionId: String? = null,
    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    var status: TransactionStatus,
    @Column(name = "fireblocks_status")
    var fireblocksStatus: String? = null,
    @Column(name = "fireblocks_sub_status")
    var fireblocksSubStatus: String? = null,
    @Column(name = "asset_id", nullable = false)
    val assetId: String,
    @Column(name = "currency", nullable = false)
    val currency: String,
    @Column(name = "protocol", nullable = false)
    val protocol: String,
    @Column(name = "amount", nullable = false, precision = Precision.PRECISION, scale = Precision.SCALE)
    val amount: BigDecimal,
    @Column(name = "source_vault_id", nullable = false)
    val sourceVaultId: String,
    @Column(name = "destination_address", nullable = false)
    val destinationAddress: String,
    @Column(name = "fee_level", nullable = false)
    @Enumerated(EnumType.STRING)
    val feeLevel: FeeLevel,
    @Column(name = "treat_as_gross_amount", nullable = false)
    val treatAsGrossAmount: Boolean,
    @Column(name = "tx_hash")
    var txHash: String? = null,
    @Column(name = "note")
    val note: String? = null,
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant,
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant,
    @Version
    var version: Long = 0,
)
