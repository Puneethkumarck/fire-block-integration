package com.stablecoin.custody.fireblocks.domain.reconciliation

import java.util.UUID

interface ReconciliationResultRepository {
    fun save(result: ReconciliationResult): ReconciliationResult

    fun findLatestByVaultAndAsset(
        vaultId: UUID,
        currency: String,
        protocol: String,
    ): ReconciliationResult?
}
