package com.stablecoin.custody.fireblocks.infrastructure.persistence

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

internal interface DepositAddressJpaRepository : JpaRepository<DepositAddressEntity, UUID> {
    fun findByWalletAssetId(walletAssetId: UUID): DepositAddressEntity?
}
