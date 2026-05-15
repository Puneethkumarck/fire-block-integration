package com.stablecoin.custody.fireblocks.infrastructure.persistence

import com.stablecoin.custody.fireblocks.domain.audit.AuditLog
import com.stablecoin.custody.fireblocks.domain.audit.AuditOperation
import com.stablecoin.custody.fireblocks.domain.audit.AuditRepository
import com.stablecoin.custody.fireblocks.domain.audit.AuditStatus
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "audit_logs")
private class AuditLogEntity(
    @Id
    @Column(name = "id", nullable = false)
    val id: UUID,
    @Column(name = "timestamp", nullable = false)
    val timestamp: Instant,
    @Column(name = "actor", nullable = false)
    val actor: String,
    @Column(name = "operation", nullable = false)
    @Enumerated(EnumType.STRING)
    val operation: AuditOperation,
    @Column(name = "resource_id", nullable = false)
    val resourceId: String,
    @Column(name = "fireblocks_request_id")
    val fireblocksRequestId: String?,
    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    val status: AuditStatus,
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "details", columnDefinition = "jsonb")
    val details: Map<String, Any>?,
)

private interface AuditLogJpaRepository : JpaRepository<AuditLogEntity, UUID> {
    fun findByResourceId(resourceId: String): List<AuditLogEntity>
}

@Component
private class AuditLogRepositoryAdapter(
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
