package com.stablecoin.custody.fireblocks.infrastructure.persistence

import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant
import java.util.UUID

internal interface FundAllocationJpaRepository : JpaRepository<FundAllocationEntity, UUID> {
    fun findByAllocationId(allocationId: String): FundAllocationEntity?

    fun findByTransactionId(transactionId: UUID): FundAllocationEntity?

    @Query("SELECT f FROM FundAllocationEntity f WHERE f.status = 'LOCKED' AND f.createdAt < :cutoff")
    fun findOrphanedLocked(
        @Param("cutoff") cutoff: Instant,
        pageable: Pageable,
    ): List<FundAllocationEntity>

    @Query("SELECT f FROM FundAllocationEntity f WHERE f.status = 'PENDING' AND f.createdAt < :cutoff")
    fun findOrphanedPending(
        @Param("cutoff") cutoff: Instant,
        pageable: Pageable,
    ): List<FundAllocationEntity>
}
