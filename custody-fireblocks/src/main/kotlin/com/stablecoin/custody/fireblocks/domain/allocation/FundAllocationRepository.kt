package com.stablecoin.custody.fireblocks.domain.allocation

import java.time.Instant
import java.util.UUID

interface FundAllocationRepository {
    fun save(allocation: FundAllocation): FundAllocation

    fun findById(id: UUID): FundAllocation?

    fun findByAllocationId(allocationId: String): FundAllocation?

    fun findByTransactionId(transactionId: UUID): FundAllocation?

    fun findOrphanedLocked(
        cutoff: Instant,
        limit: Int,
    ): List<FundAllocation>

    fun findOrphanedPending(
        cutoff: Instant,
        limit: Int,
    ): List<FundAllocation>
}
