package com.stablecoin.custody.fireblocks.domain.webhook

import com.stablecoin.custody.fireblocks.domain.audit.AuditLog
import com.stablecoin.custody.fireblocks.domain.audit.AuditLogRepository
import com.stablecoin.custody.fireblocks.domain.audit.AuditOperation
import com.stablecoin.custody.fireblocks.domain.audit.AuditStatus
import com.stablecoin.custody.fireblocks.domain.shared.logger
import com.stablecoin.custody.fireblocks.domain.transaction.TransactionStatusHandler
import org.springframework.stereotype.Service

private val log = logger<WebhookEventHandler>()

@Service
class WebhookEventHandler(
    private val transactionStatusHandler: TransactionStatusHandler,
    private val auditLogRepository: AuditLogRepository,
) {
    fun handle(payload: WebhookPayload) {
        when (payload.type) {
            "TRANSACTION_STATUS_UPDATED" -> handleTransactionStatusUpdated(payload)
            else -> log.warn("Ignoring unknown webhook type: {}", payload.type)
        }

        auditLogRepository.save(
            AuditLog.create(
                operation = AuditOperation.WEBHOOK_RECEIVED,
                actor = "fireblocks",
                resourceId = payload.data?.id ?: "unknown",
                status = AuditStatus.SUCCESS,
                details = mapOf("type" to payload.type),
            ),
        )
    }

    private fun handleTransactionStatusUpdated(payload: WebhookPayload) {
        val data =
            payload.data ?: run {
                log.warn("Webhook missing data: type={}", payload.type)
                return
            }
        transactionStatusHandler.handleStatusUpdate(
            fireblocksTxId = data.id,
            fireblocksStatus = data.status,
            subStatus = data.subStatus,
            txHash = data.txHash,
        )
    }
}
