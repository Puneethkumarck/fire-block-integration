package com.stablecoin.custody.fireblocks.infrastructure.persistence

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

internal interface InternalBalanceJpaRepository : JpaRepository<InternalBalanceEntity, UUID> {
    fun findByVaultIdAndCurrencyAndProtocol(
        vaultId: UUID,
        currency: String,
        protocol: String,
    ): InternalBalanceEntity?
}
