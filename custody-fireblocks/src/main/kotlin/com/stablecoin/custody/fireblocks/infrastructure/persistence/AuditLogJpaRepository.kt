package com.stablecoin.custody.fireblocks.infrastructure.persistence

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

internal interface AuditLogJpaRepository : JpaRepository<AuditLogEntity, UUID> {
    fun findByResourceIdOrderByTimestampAscIdAsc(resourceId: String): List<AuditLogEntity>
}
