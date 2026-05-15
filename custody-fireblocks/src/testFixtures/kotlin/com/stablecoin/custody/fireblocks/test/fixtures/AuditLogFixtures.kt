package com.stablecoin.custody.fireblocks.test.fixtures

import com.stablecoin.custody.fireblocks.domain.audit.AuditLog
import com.stablecoin.custody.fireblocks.domain.audit.AuditOperation
import com.stablecoin.custody.fireblocks.domain.audit.AuditStatus
import java.time.Instant
import java.util.UUID

fun anAuditLog(
    id: UUID = UUID.randomUUID(),
    timestamp: Instant = Instant.now(),
    actor: String = "system",
    operation: AuditOperation = AuditOperation.VAULT_CREATED,
    resourceId: String = "resource-${UUID.randomUUID()}",
    fireblocksRequestId: String? = "fb-req-123",
    status: AuditStatus = AuditStatus.SUCCESS,
    details: Map<String, Any>? = null,
) = AuditLog(
    id = id,
    timestamp = timestamp,
    actor = actor,
    operation = operation,
    resourceId = resourceId,
    fireblocksRequestId = fireblocksRequestId,
    status = status,
    details = details,
)
