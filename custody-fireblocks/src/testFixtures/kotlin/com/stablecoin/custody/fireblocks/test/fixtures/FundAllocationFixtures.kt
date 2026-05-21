package com.stablecoin.custody.fireblocks.test.fixtures

import com.stablecoin.custody.fireblocks.domain.allocation.AllocationStatus
import com.stablecoin.custody.fireblocks.domain.allocation.FundAllocation
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

fun aFundAllocation(
    id: UUID = UUID.randomUUID(),
    allocationId: String = "alloc-${UUID.randomUUID()}",
    vaultId: UUID = UUID.randomUUID(),
    fireblocksVaultId: String = "fb-vault-123",
    assetId: String = "BTC",
    currency: String = "BTC",
    protocol: String = "BTC",
    amount: BigDecimal = BigDecimal("1.5"),
    status: AllocationStatus = AllocationStatus.PENDING,
    transactionId: UUID? = null,
    createdAt: Instant = Instant.now(),
    updatedAt: Instant = createdAt,
    version: Long = 0,
) = FundAllocation(
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
