package com.stablecoin.custody.fireblocks.infrastructure.persistence

import com.stablecoin.custody.fireblocks.domain.reconciliation.InternalBalance
import com.stablecoin.custody.fireblocks.domain.reconciliation.InternalBalanceRepository
import org.springframework.stereotype.Component
import java.util.UUID

@Component
internal class InternalBalanceRepositoryAdapter(
    private val jpaRepository: InternalBalanceJpaRepository,
) : InternalBalanceRepository {
    override fun findByVaultIdAndCurrencyAndProtocol(
        vaultId: UUID,
        currency: String,
        protocol: String,
    ): InternalBalance? =
        jpaRepository
            .findByVaultIdAndCurrencyAndProtocol(vaultId, currency, protocol)
            ?.toDomain()

    override fun save(balance: InternalBalance): InternalBalance = balance.toEntity().let { jpaRepository.saveAndFlush(it) }.toDomain()

    fun InternalBalance.toEntity() =
        InternalBalanceEntity(
            id = id,
            vaultId = vaultId,
            currency = currency,
            protocol = protocol,
            balance = balance,
            lastTransactionId = lastTransactionId,
            updatedAt = updatedAt,
            version = version,
        )

    fun InternalBalanceEntity.toDomain() =
        InternalBalance(
            id = id,
            vaultId = vaultId,
            currency = currency,
            protocol = protocol,
            balance = balance,
            lastTransactionId = lastTransactionId,
            updatedAt = updatedAt,
            version = version,
        )
}
