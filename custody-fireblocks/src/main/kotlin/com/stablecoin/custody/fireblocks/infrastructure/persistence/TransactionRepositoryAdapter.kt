package com.stablecoin.custody.fireblocks.infrastructure.persistence

import com.stablecoin.custody.fireblocks.domain.transaction.Transaction
import com.stablecoin.custody.fireblocks.domain.transaction.TransactionId
import com.stablecoin.custody.fireblocks.domain.transaction.TransactionRepository
import com.stablecoin.custody.fireblocks.domain.transaction.TransactionStatus
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Component
import java.time.Instant

@Component
internal class TransactionRepositoryAdapter(
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
                terminalStatuses = TransactionStatus.entries.filter { it.terminal }.toSet(),
                cutoff = cutoff,
                pageable = PageRequest.of(0, limit, Sort.by("updatedAt").ascending()),
            ).map { it.toDomain() }

    override fun findStaleCreated(
        cutoff: Instant,
        limit: Int,
    ): List<Transaction> =
        jpaRepository
            .findStaleCreated(
                status = TransactionStatus.CREATED,
                cutoff = cutoff,
                pageable = PageRequest.of(0, limit, Sort.by("createdAt").ascending()),
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
