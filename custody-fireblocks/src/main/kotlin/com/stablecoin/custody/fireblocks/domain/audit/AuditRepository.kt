package com.stablecoin.custody.fireblocks.domain.audit

interface AuditRepository {
    fun save(auditLog: AuditLog): AuditLog

    fun findByResourceId(resourceId: String): List<AuditLog>
}
