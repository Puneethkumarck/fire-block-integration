package com.stablecoin.custody.fireblocks.application.webhook

import com.stablecoin.custody.fireblocks.domain.audit.AuditLog
import com.stablecoin.custody.fireblocks.domain.audit.AuditLogRepository
import com.stablecoin.custody.fireblocks.domain.audit.AuditOperation
import com.stablecoin.custody.fireblocks.domain.audit.AuditStatus
import com.stablecoin.custody.fireblocks.domain.shared.logger
import com.stablecoin.custody.fireblocks.domain.transaction.TransactionStatusHandler
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

private val log = logger<FireblocksWebhookController>()

@RestController
@RequestMapping("/api/v1/webhooks")
class FireblocksWebhookController(
    private val transactionStatusHandler: TransactionStatusHandler,
    private val auditLogRepository: AuditLogRepository,
) {
    @PostMapping("/fireblocks")
    fun handleWebhook(
        @RequestBody payload: WebhookPayload,
    ): ResponseEntity<Void> {
        log.info("Webhook received: type={}, fireblocksTxId={}", payload.type, payload.data?.id)

        when (payload.type) {
            "TRANSACTION_STATUS_UPDATED" -> {
                val data =
                    payload.data ?: run {
                        log.warn("Webhook missing data: type={}", payload.type)
                        return ResponseEntity.ok().build()
                    }
                transactionStatusHandler.handleStatusUpdate(
                    fireblocksTxId = data.id,
                    fireblocksStatus = data.status,
                    subStatus = data.subStatus,
                    txHash = data.txHash,
                )
            }
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

        return ResponseEntity.ok().build()
    }
}
