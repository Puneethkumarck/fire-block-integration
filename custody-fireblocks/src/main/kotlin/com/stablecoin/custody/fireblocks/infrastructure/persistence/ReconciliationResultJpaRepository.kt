package com.stablecoin.custody.fireblocks.infrastructure.persistence

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

internal interface ReconciliationResultJpaRepository : JpaRepository<ReconciliationResultEntity, UUID> {
    fun findFirstByVaultIdAndCurrencyAndProtocolOrderByCreatedAtDesc(
        vaultId: UUID,
        currency: String,
        protocol: String,
    ): ReconciliationResultEntity?
}
