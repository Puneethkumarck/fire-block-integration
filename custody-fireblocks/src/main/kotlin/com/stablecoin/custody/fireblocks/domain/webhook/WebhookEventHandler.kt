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
        var auditStatus = AuditStatus.SUCCESS
        try {
            when (payload.type) {
                "TRANSACTION_STATUS_UPDATED" -> handleTransactionStatusUpdated(payload)
                else -> log.warn("Ignoring unknown webhook type: {}", payload.type)
            }
        } catch (ex: Exception) {
            auditStatus = AuditStatus.FAILURE
            log.error("Webhook handling failed: type={}, fireblocksTxId={}", payload.type, payload.data?.id, ex)
        } finally {
            runCatching {
                auditLogRepository.save(
                    AuditLog.create(
                        operation = AuditOperation.WEBHOOK_RECEIVED,
                        actor = "fireblocks",
                        resourceId = payload.data?.id ?: "unknown",
                        status = auditStatus,
                        details = mapOf("type" to payload.type),
                    ),
                )
            }.onFailure { ex ->
                log.error("Failed to persist webhook audit log: type={}", payload.type, ex)
            }
        }
    }

    private fun handleTransactionStatusUpdated(payload: WebhookPayload) {
        val data =
            payload.data ?: run {
                log.warn("Webhook missing data: type={}", payload.type)
                return
            }
        val id =
            data.id ?: run {
                log.warn("Webhook transaction data missing id")
                return
            }
        val status =
            data.status ?: run {
                log.warn("Webhook transaction data missing status: id={}", id)
                return
            }
        transactionStatusHandler.handleStatusUpdate(
            fireblocksTxId = id,
            fireblocksStatus = status,
            subStatus = data.subStatus,
            txHash = data.txHash,
        )
    }
}
