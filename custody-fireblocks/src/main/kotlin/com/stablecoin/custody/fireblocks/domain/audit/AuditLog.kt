package com.stablecoin.custody.fireblocks.domain.audit

import java.time.Instant
import java.util.UUID

data class AuditLog(
    val id: UUID,
    val timestamp: Instant,
    val actor: String,
    val operation: AuditOperation,
    val resourceId: String,
    val fireblocksRequestId: String?,
    val status: AuditStatus,
    val details: Map<String, Any>?,
) {
    init {
        require(actor.isNotBlank()) { "actor must not be blank" }
        require(resourceId.isNotBlank()) { "resourceId must not be blank" }
        require(fireblocksRequestId == null || fireblocksRequestId.isNotBlank()) { "fireblocksRequestId must not be blank" }
    }

    companion object {
        fun create(
            operation: AuditOperation,
            actor: String,
            resourceId: String,
            status: AuditStatus,
            fireblocksRequestId: String? = null,
            details: Map<String, Any>? = null,
        ) = AuditLog(
            id = UUID.randomUUID(),
            timestamp = Instant.now(),
            operation = operation,
            actor = actor,
            resourceId = resourceId,
            fireblocksRequestId = fireblocksRequestId,
            status = status,
            details = details,
        )
    }
}
