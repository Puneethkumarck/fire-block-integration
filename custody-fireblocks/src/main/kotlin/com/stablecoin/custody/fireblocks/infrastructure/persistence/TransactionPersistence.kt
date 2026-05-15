package com.stablecoin.custody.fireblocks.infrastructure.persistence

import com.stablecoin.custody.fireblocks.domain.shared.Precision
import com.stablecoin.custody.fireblocks.domain.transaction.FeeLevel
import com.stablecoin.custody.fireblocks.domain.transaction.Transaction
import com.stablecoin.custody.fireblocks.domain.transaction.TransactionId
import com.stablecoin.custody.fireblocks.domain.transaction.TransactionRepository
import com.stablecoin.custody.fireblocks.domain.transaction.TransactionStatus
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.LockModeType
import jakarta.persistence.Table
import jakarta.persistence.Version
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "transactions")
private class TransactionEntity(
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
    var feeLevel: FeeLevel,
    @Column(name = "treat_as_gross_amount", nullable = false)
    var treatAsGrossAmount: Boolean,
    @Column(name = "tx_hash")
    var txHash: String? = null,
    @Column(name = "note")
    var note: String? = null,
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant,
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant,
    @Version
    var version: Long = 0,
)

private interface TransactionJpaRepository : JpaRepository<TransactionEntity, UUID> {
    fun findByExternalTxId(externalTxId: String): TransactionEntity?

    fun findByFireblocksTransactionId(fireblocksTxId: String): TransactionEntity?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM TransactionEntity t WHERE t.id = :id")
    fun findByIdForUpdate(
        @Param("id") id: UUID,
    ): TransactionEntity?

    @Query(
        "SELECT t FROM TransactionEntity t WHERE t.status NOT IN :terminalStatuses AND t.updatedAt < :cutoff ORDER BY t.updatedAt ASC LIMIT :limit",
    )
    fun findStaleNonTerminal(
        @Param("terminalStatuses") terminalStatuses: Set<TransactionStatus>,
        @Param("cutoff") cutoff: Instant,
        @Param("limit") limit: Int,
    ): List<TransactionEntity>

    @Query(
        "SELECT t FROM TransactionEntity t WHERE t.status = :status AND t.createdAt < :cutoff ORDER BY t.createdAt ASC LIMIT :limit",
    )
    fun findStaleCreated(
        @Param("status") status: TransactionStatus,
        @Param("cutoff") cutoff: Instant,
        @Param("limit") limit: Int,
    ): List<TransactionEntity>
}

@Component
private class TransactionRepositoryAdapter(
    private val jpaRepository: TransactionJpaRepository,
) : TransactionRepository {
    override fun findById(id: TransactionId): Transaction? =
        jpaRepository
            .findById(id.value)
            .map { it.toDomain() }
            .orElse(null)

    override fun findByExternalTxId(externalTxId: String): Transaction? = jpaRepository.findByExternalTxId(externalTxId)?.toDomain()

    override fun findByFireblocksTransactionId(fireblocksTxId: String): Transaction? =
        jpaRepository.findByFireblocksTransactionId(fireblocksTxId)?.toDomain()

    override fun save(transaction: Transaction): Transaction = transaction.toEntity().let { jpaRepository.saveAndFlush(it) }.toDomain()

    override fun findByIdForUpdate(id: TransactionId): Transaction? = jpaRepository.findByIdForUpdate(id.value)?.toDomain()

    override fun findStaleNonTerminal(
        cutoff: Instant,
        limit: Int,
    ): List<Transaction> =
        jpaRepository
            .findStaleNonTerminal(
                terminalStatuses = setOf(TransactionStatus.CONFIRMED, TransactionStatus.FAILED),
                cutoff = cutoff,
                limit = limit,
            ).map { it.toDomain() }

    override fun findStaleCreated(
        cutoff: Instant,
        limit: Int,
    ): List<Transaction> =
        jpaRepository
            .findStaleCreated(
                status = TransactionStatus.CREATED,
                cutoff = cutoff,
                limit = limit,
            ).map { it.toDomain() }

    fun Transaction.toEntity() =
        TransactionEntity(
            id = id.value,
            externalTxId = externalTxId,
            fireblocksTransactionId = fireblocksTransactionId,
            status = status,
            fireblocksStatus = fireblocksStatus,
            fireblocksSubStatus = fireblocksSubStatus,
            assetId = assetId,
            currency = currency,
            protocol = protocol,
            amount = amount,
            sourceVaultId = sourceVaultId,
            destinationAddress = destinationAddress,
            feeLevel = feeLevel,
            treatAsGrossAmount = treatAsGrossAmount,
            txHash = txHash,
            note = note,
            createdAt = createdAt,
            updatedAt = updatedAt,
            version = version,
        )

    fun TransactionEntity.toDomain() =
        Transaction(
            id = TransactionId(id),
            externalTxId = externalTxId,
            fireblocksTransactionId = fireblocksTransactionId,
            status = status,
            fireblocksStatus = fireblocksStatus,
            fireblocksSubStatus = fireblocksSubStatus,
            assetId = assetId,
            currency = currency,
            protocol = protocol,
            amount = amount,
            sourceVaultId = sourceVaultId,
            destinationAddress = destinationAddress,
            feeLevel = feeLevel,
            treatAsGrossAmount = treatAsGrossAmount,
            txHash = txHash,
            note = note,
            createdAt = createdAt,
            updatedAt = updatedAt,
            version = version,
        )
}
