package com.stablecoin.custody.fireblocks.infrastructure.persistence

import com.stablecoin.custody.fireblocks.domain.reconciliation.ReconciliationResult
import com.stablecoin.custody.fireblocks.domain.reconciliation.ReconciliationResultRepository
import org.springframework.stereotype.Component
import java.util.UUID

@Component
internal class ReconciliationResultRepositoryAdapter(
    private val jpaRepository: ReconciliationResultJpaRepository,
) : ReconciliationResultRepository {
    override fun save(result: ReconciliationResult): ReconciliationResult =
        result.toEntity().let { jpaRepository.saveAndFlush(it) }.toDomain()

    override fun findLatestByVaultAndAsset(
        vaultId: UUID,
        currency: String,
        protocol: String,
    ): ReconciliationResult? =
        jpaRepository
            .findFirstByVaultIdAndCurrencyAndProtocolOrderByCreatedAtDesc(vaultId, currency, protocol)
            ?.toDomain()

    fun ReconciliationResult.toEntity() =
        ReconciliationResultEntity(
            id = id,
            vaultId = vaultId,
            currency = currency,
            protocol = protocol,
            internalBalance = internalBalance,
            fireblocksBalance = fireblocksBalance,
            drift = drift,
            absoluteDrift = absoluteDrift,
            status = status,
            toleranceUsed = toleranceUsed,
            createdAt = createdAt,
        )

    fun ReconciliationResultEntity.toDomain() =
        ReconciliationResult(
            id = id,
            vaultId = vaultId,
            currency = currency,
            protocol = protocol,
            internalBalance = internalBalance,
            fireblocksBalance = fireblocksBalance,
            drift = drift,
            absoluteDrift = absoluteDrift,
            status = status,
            toleranceUsed = toleranceUsed,
            createdAt = createdAt,
        )
}
