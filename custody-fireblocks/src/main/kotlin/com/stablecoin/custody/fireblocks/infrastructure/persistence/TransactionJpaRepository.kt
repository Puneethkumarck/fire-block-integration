package com.stablecoin.custody.fireblocks.infrastructure.persistence

import com.stablecoin.custody.fireblocks.domain.transaction.TransactionStatus
import jakarta.persistence.LockModeType
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant
import java.util.UUID

internal interface TransactionJpaRepository : JpaRepository<TransactionEntity, UUID> {
    fun findByExternalTxId(externalTxId: String): TransactionEntity?

    fun findByFireblocksTransactionId(fireblocksTxId: String): TransactionEntity?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM TransactionEntity t WHERE t.id = :id")
    fun findByIdForUpdate(
        @Param("id") id: UUID,
    ): TransactionEntity?

    @Query(
        "SELECT t FROM TransactionEntity t WHERE t.status NOT IN :terminalStatuses AND t.updatedAt < :cutoff",
    )
    fun findStaleNonTerminal(
        @Param("terminalStatuses") terminalStatuses: Set<TransactionStatus>,
        @Param("cutoff") cutoff: Instant,
        pageable: Pageable,
    ): List<TransactionEntity>

    @Query(
        "SELECT t FROM TransactionEntity t WHERE t.status = :status AND t.createdAt < :cutoff",
    )
    fun findStaleCreated(
        @Param("status") status: TransactionStatus,
        @Param("cutoff") cutoff: Instant,
        pageable: Pageable,
    ): List<TransactionEntity>
}
