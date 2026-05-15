package com.stablecoin.custody.fireblocks.domain.audit

interface AuditLogRepository {
    fun save(auditLog: AuditLog): AuditLog

    fun findByResourceId(resourceId: String): List<AuditLog>
}
