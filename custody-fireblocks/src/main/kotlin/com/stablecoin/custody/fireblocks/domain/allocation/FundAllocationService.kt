package com.stablecoin.custody.fireblocks.domain.allocation

import com.stablecoin.custody.fireblocks.domain.audit.AuditLog
import com.stablecoin.custody.fireblocks.domain.audit.AuditLogRepository
import com.stablecoin.custody.fireblocks.domain.audit.AuditOperation
import com.stablecoin.custody.fireblocks.domain.audit.AuditStatus
import com.stablecoin.custody.fireblocks.domain.exception.AllocationAlreadyReleasedException
import com.stablecoin.custody.fireblocks.domain.exception.AllocationNotFoundException
import com.stablecoin.custody.fireblocks.domain.port.FireblocksVaultPort
import com.stablecoin.custody.fireblocks.domain.port.LockAllocationCommand
import com.stablecoin.custody.fireblocks.domain.port.ReleaseAllocationCommand
import com.stablecoin.custody.fireblocks.domain.shared.logger
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

private val log = logger<FundAllocationService>()

@Service
class FundAllocationService(
    private val fundAllocationRepository: FundAllocationRepository,
    private val fireblocksVaultPort: FireblocksVaultPort,
    private val auditLogRepository: AuditLogRepository,
) {
    @Transactional
    fun createAndLock(
        allocationId: String,
        vaultId: UUID,
        fireblocksVaultId: String,
        assetId: String,
        currency: String,
        protocol: String,
        amount: BigDecimal,
    ): FundAllocation {
        log.info("Creating and locking allocation: allocationId={}", allocationId)

        fundAllocationRepository.findByAllocationId(allocationId)?.let {
            log.info("Allocation already exists: allocationId={}", allocationId)
            return it
        }

        val allocation =
            FundAllocation.create(
                allocationId = allocationId,
                vaultId = vaultId,
                fireblocksVaultId = fireblocksVaultId,
                assetId = assetId,
                currency = currency,
                protocol = protocol,
                amount = amount,
            )
        val saved = fundAllocationRepository.save(allocation)

        return try {
            fireblocksVaultPort.lockAllocation(
                LockAllocationCommand(
                    allocationId = allocationId,
                    vaultAccountId = fireblocksVaultId,
                    assetId = assetId,
                    amount = amount,
                ),
            )

            val locked = saved.lock()
            val result = fundAllocationRepository.save(locked)

            auditLogRepository.save(
                AuditLog.create(
                    operation = AuditOperation.ALLOCATION_LOCKED,
                    actor = "system",
                    resourceId = result.id.toString(),
                    status = AuditStatus.SUCCESS,
                    details = mapOf("allocationId" to allocationId, "amount" to amount.toPlainString()),
                ),
            )

            result
        } catch (e: Exception) {
            log.error("Failed to lock allocation: allocationId={}", allocationId, e)

            val failed = saved.markFailed()
            val result = fundAllocationRepository.save(failed)

            auditLogRepository.save(
                AuditLog.create(
                    operation = AuditOperation.ALLOCATION_LOCKED,
                    actor = "system",
                    resourceId = result.id.toString(),
                    status = AuditStatus.FAILURE,
                    details = mapOf("allocationId" to allocationId, "error" to (e.message ?: "unknown")),
                ),
            )

            result
        }
    }

    @Transactional
    fun consume(
        allocationId: String,
        transactionId: UUID,
    ): FundAllocation {
        log.info("Consuming allocation: allocationId={}, transactionId={}", allocationId, transactionId)

        val allocation =
            fundAllocationRepository.findByAllocationId(allocationId)
                ?: throw AllocationNotFoundException(allocationId)

        val consumed = allocation.consume(transactionId)
        val result = fundAllocationRepository.save(consumed)

        auditLogRepository.save(
            AuditLog.create(
                operation = AuditOperation.ALLOCATION_CONSUMED,
                actor = "system",
                resourceId = result.id.toString(),
                status = AuditStatus.SUCCESS,
                details = mapOf("allocationId" to allocationId, "transactionId" to transactionId.toString()),
            ),
        )

        return result
    }

    @Transactional
    fun release(allocationId: String): FundAllocation {
        log.info("Releasing allocation: allocationId={}", allocationId)

        val allocation =
            fundAllocationRepository.findByAllocationId(allocationId)
                ?: throw AllocationNotFoundException(allocationId)

        if (allocation.status == AllocationStatus.RELEASED || allocation.status == AllocationStatus.FAILED) {
            throw AllocationAlreadyReleasedException(allocationId)
        }

        check(AllocationStatus.canTransition(allocation.status, AllocationStatus.RELEASED)) {
            "Cannot release allocation $allocationId from status ${allocation.status}"
        }

        fireblocksVaultPort.releaseAllocation(
            ReleaseAllocationCommand(
                allocationId = allocationId,
                vaultAccountId = allocation.fireblocksVaultId,
                assetId = allocation.assetId,
            ),
        )

        val released = allocation.release()
        val result = fundAllocationRepository.save(released)

        auditLogRepository.save(
            AuditLog.create(
                operation = AuditOperation.ALLOCATION_RELEASED,
                actor = "system",
                resourceId = result.id.toString(),
                status = AuditStatus.SUCCESS,
                details = mapOf("allocationId" to allocationId),
            ),
        )

        return result
    }

    @Transactional
    fun releaseByTransactionId(transactionId: UUID) {
        log.info("Releasing allocation by transactionId: transactionId={}", transactionId)

        val allocation = fundAllocationRepository.findByTransactionId(transactionId) ?: return

        if (allocation.status.terminal) {
            log.info("Allocation already terminal: allocationId={}, status={}", allocation.allocationId, allocation.status)
            return
        }

        fireblocksVaultPort.releaseAllocation(
            ReleaseAllocationCommand(
                allocationId = allocation.allocationId,
                vaultAccountId = allocation.fireblocksVaultId,
                assetId = allocation.assetId,
            ),
        )

        val released = allocation.release()
        fundAllocationRepository.save(released)

        auditLogRepository.save(
            AuditLog.create(
                operation = AuditOperation.ALLOCATION_RELEASED,
                actor = "system",
                resourceId = released.id.toString(),
                status = AuditStatus.SUCCESS,
                details =
                    mapOf(
                        "allocationId" to allocation.allocationId,
                        "transactionId" to transactionId.toString(),
                    ),
            ),
        )
    }

    @Transactional
    fun releaseOrphanedLocked(
        cutoff: Instant,
        limit: Int,
    ) {
        val orphaned = fundAllocationRepository.findOrphanedLocked(cutoff, limit)
        log.info("Found {} orphaned locked allocations to release", orphaned.size)

        orphaned.forEach { allocation ->
            try {
                fireblocksVaultPort.releaseAllocation(
                    ReleaseAllocationCommand(
                        allocationId = allocation.allocationId,
                        vaultAccountId = allocation.fireblocksVaultId,
                        assetId = allocation.assetId,
                    ),
                )

                val released = allocation.release()
                fundAllocationRepository.save(released)

                auditLogRepository.save(
                    AuditLog.create(
                        operation = AuditOperation.ALLOCATION_RELEASED_BY_CLEANUP,
                        actor = "system",
                        resourceId = released.id.toString(),
                        status = AuditStatus.SUCCESS,
                        details = mapOf("allocationId" to allocation.allocationId),
                    ),
                )
            } catch (e: Exception) {
                log.error("Failed to release orphaned allocation: allocationId={}", allocation.allocationId, e)
            }
        }
    }

    @Transactional
    fun failOrphanedPending(
        cutoff: Instant,
        limit: Int,
    ) {
        val orphaned = fundAllocationRepository.findOrphanedPending(cutoff, limit)
        log.info("Found {} orphaned pending allocations to fail", orphaned.size)

        orphaned.forEach { allocation ->
            try {
                val failed = allocation.markFailed()
                fundAllocationRepository.save(failed)

                auditLogRepository.save(
                    AuditLog.create(
                        operation = AuditOperation.ALLOCATION_FAILED_BY_CLEANUP,
                        actor = "system",
                        resourceId = failed.id.toString(),
                        status = AuditStatus.SUCCESS,
                        details = mapOf("allocationId" to allocation.allocationId),
                    ),
                )
            } catch (e: Exception) {
                log.error("Failed to mark orphaned allocation as failed: allocationId={}", allocation.allocationId, e)
            }
        }
    }
}
