package com.stablecoin.custody.fireblocks.domain.allocation

import com.stablecoin.custody.fireblocks.domain.shared.StateProvider
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

data class FundAllocation(
    val id: UUID,
    val allocationId: String,
    val vaultId: UUID,
    val fireblocksVaultId: String,
    val assetId: String,
    val currency: String,
    val protocol: String,
    val amount: BigDecimal,
    val status: AllocationStatus,
    val transactionId: UUID?,
    val createdAt: Instant,
    val updatedAt: Instant,
    val version: Long = 0,
) : StateProvider<AllocationStatus> {
    init {
        require(allocationId.isNotBlank()) { "allocationId must not be blank" }
        require(fireblocksVaultId.isNotBlank()) { "fireblocksVaultId must not be blank" }
        require(assetId.isNotBlank()) { "assetId must not be blank" }
        require(currency.isNotBlank()) { "currency must not be blank" }
        require(protocol.isNotBlank()) { "protocol must not be blank" }
        require(amount > BigDecimal.ZERO) { "amount must be positive" }
    }

    override fun currentState() = status

    fun lock(): FundAllocation {
        guardTransition(AllocationStatus.LOCKED)
        return copy(
            status = AllocationStatus.LOCKED,
            updatedAt = Instant.now(),
        )
    }

    fun consume(transactionId: UUID): FundAllocation {
        guardTransition(AllocationStatus.CONSUMED)
        return copy(
            status = AllocationStatus.CONSUMED,
            transactionId = transactionId,
            updatedAt = Instant.now(),
        )
    }

    fun release(): FundAllocation {
        guardTransition(AllocationStatus.RELEASED)
        return copy(
            status = AllocationStatus.RELEASED,
            updatedAt = Instant.now(),
        )
    }

    fun markFailed(): FundAllocation {
        guardTransition(AllocationStatus.FAILED)
        return copy(
            status = AllocationStatus.FAILED,
            updatedAt = Instant.now(),
        )
    }

    private fun guardTransition(target: AllocationStatus) {
        check(AllocationStatus.canTransition(status, target)) {
            "Cannot transition allocation $allocationId from $status to $target"
        }
    }

    companion object {
        fun create(
            allocationId: String,
            vaultId: UUID,
            fireblocksVaultId: String,
            assetId: String,
            currency: String,
            protocol: String,
            amount: BigDecimal,
        ) = FundAllocation(
            id = UUID.randomUUID(),
            allocationId = allocationId,
            vaultId = vaultId,
            fireblocksVaultId = fireblocksVaultId,
            assetId = assetId,
            currency = currency,
            protocol = protocol,
            amount = amount,
            status = AllocationStatus.PENDING,
            transactionId = null,
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
        )
    }
}
