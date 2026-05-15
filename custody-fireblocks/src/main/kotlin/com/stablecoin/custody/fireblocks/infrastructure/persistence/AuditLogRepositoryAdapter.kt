package com.stablecoin.custody.fireblocks.infrastructure.persistence

import com.stablecoin.custody.fireblocks.domain.audit.AuditLog
import com.stablecoin.custody.fireblocks.domain.audit.AuditRepository
import org.springframework.stereotype.Component

@Component
internal class AuditLogRepositoryAdapter(
    private val jpaRepository: AuditLogJpaRepository,
) : AuditRepository {
    override fun save(auditLog: AuditLog): AuditLog = auditLog.toEntity().let { jpaRepository.saveAndFlush(it) }.toDomain()

    override fun findByResourceId(resourceId: String): List<AuditLog> = jpaRepository.findByResourceId(resourceId).map { it.toDomain() }

    fun AuditLog.toEntity() =
        AuditLogEntity(
            id = id,
            timestamp = timestamp,
            actor = actor,
            operation = operation,
            resourceId = resourceId,
            fireblocksRequestId = fireblocksRequestId,
            status = status,
            details = details,
        )

    fun AuditLogEntity.toDomain() =
        AuditLog(
            id = id,
            timestamp = timestamp,
            actor = actor,
            operation = operation,
            resourceId = resourceId,
            fireblocksRequestId = fireblocksRequestId,
            status = status,
            details = details,
        )
}
