package com.stablecoin.custody.fireblocks.infrastructure.persistence

import com.stablecoin.custody.fireblocks.domain.vault.VaultStatus
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.time.Instant
import java.util.UUID

internal interface VaultJpaRepository : JpaRepository<VaultEntity, UUID> {
    fun findByCustomerRefId(customerRefId: String): VaultEntity?

    @Query("SELECT v FROM VaultEntity v WHERE v.status = :status AND v.createdAt < :cutoff ORDER BY v.createdAt ASC")
    fun findPendingOlderThan(
        status: VaultStatus,
        cutoff: Instant,
        pageable: Pageable,
    ): List<VaultEntity>
}
