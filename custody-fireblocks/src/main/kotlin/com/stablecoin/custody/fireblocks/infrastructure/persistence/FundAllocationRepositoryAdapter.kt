package com.stablecoin.custody.fireblocks.infrastructure.persistence

import com.stablecoin.custody.fireblocks.domain.allocation.FundAllocation
import com.stablecoin.custody.fireblocks.domain.allocation.FundAllocationRepository
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.UUID

@Component
internal class FundAllocationRepositoryAdapter(
    private val jpaRepository: FundAllocationJpaRepository,
) : FundAllocationRepository {
    override fun save(allocation: FundAllocation): FundAllocation = allocation.toEntity().let { jpaRepository.saveAndFlush(it) }.toDomain()

    override fun findById(id: UUID): FundAllocation? =
        jpaRepository
            .findById(id)
            .map { it.toDomain() }
            .orElse(null)

    override fun findByAllocationId(allocationId: String): FundAllocation? = jpaRepository.findByAllocationId(allocationId)?.toDomain()

    override fun findByTransactionId(transactionId: UUID): FundAllocation? = jpaRepository.findByTransactionId(transactionId)?.toDomain()

    override fun findOrphanedLocked(
        cutoff: Instant,
        limit: Int,
    ): List<FundAllocation> =
        jpaRepository
            .findOrphanedLocked(
                cutoff = cutoff,
                pageable = PageRequest.of(0, limit, Sort.by("createdAt").ascending()),
            ).map { it.toDomain() }

    override fun findOrphanedPending(
        cutoff: Instant,
        limit: Int,
    ): List<FundAllocation> =
        jpaRepository
            .findOrphanedPending(
                cutoff = cutoff,
                pageable = PageRequest.of(0, limit, Sort.by("createdAt").ascending()),
            ).map { it.toDomain() }

    fun FundAllocation.toEntity() =
        FundAllocationEntity(
            id = id,
            allocationId = allocationId,
            vaultId = vaultId,
            fireblocksVaultId = fireblocksVaultId,
            assetId = assetId,
            currency = currency,
            protocol = protocol,
            amount = amount,
            status = status,
            transactionId = transactionId,
            createdAt = createdAt,
            updatedAt = updatedAt,
            version = version,
        )

    fun FundAllocationEntity.toDomain() =
        FundAllocation(
            id = id,
            allocationId = allocationId,
            vaultId = vaultId,
            fireblocksVaultId = fireblocksVaultId,
            assetId = assetId,
            currency = currency,
            protocol = protocol,
            amount = amount,
            status = status,
            transactionId = transactionId,
            createdAt = createdAt,
            updatedAt = updatedAt,
            version = version,
        )
}
