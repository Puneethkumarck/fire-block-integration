package com.stablecoin.custody.fireblocks.domain.webhook

import com.stablecoin.custody.fireblocks.domain.audit.AuditLog
import com.stablecoin.custody.fireblocks.domain.audit.AuditLogRepository
import com.stablecoin.custody.fireblocks.domain.audit.AuditOperation
import com.stablecoin.custody.fireblocks.domain.transaction.TransactionStatusHandler
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class WebhookEventHandlerTest {
    private val transactionStatusHandler: TransactionStatusHandler = mockk(relaxed = true)
    private val auditLogRepository: AuditLogRepository = mockk()
    private val handler = WebhookEventHandler(transactionStatusHandler, auditLogRepository)

    @BeforeEach
    fun setUp() {
        every { auditLogRepository.save(any()) } answers { firstArg() }
    }

    @Test
    fun `should process TRANSACTION_STATUS_UPDATED and delegate to TransactionStatusHandler`() {
        // given
        val payload =
            WebhookPayload(
                type = "TRANSACTION_STATUS_UPDATED",
                tenantId = "t1",
                timestamp = 1700000000000,
                createdAt = 1700000000000,
                data =
                    WebhookTransactionData(
                        id = "fb-tx-001",
                        status = "COMPLETED",
                        subStatus = "CONFIRMED",
                        txHash = "0xhash123",
                    ),
            )

        // when
        handler.handle(payload)

        // then
        verify {
            transactionStatusHandler.handleStatusUpdate(
                fireblocksTxId = "fb-tx-001",
                fireblocksStatus = "COMPLETED",
                subStatus = "CONFIRMED",
                txHash = "0xhash123",
            )
        }
    }

    @Test
    fun `should ignore unknown webhook type without calling TransactionStatusHandler`() {
        // given
        val payload =
            WebhookPayload(
                type = "VAULT_CREATED",
                tenantId = "t1",
                timestamp = 1700000000000,
                createdAt = 1700000000000,
                data = WebhookTransactionData(id = "v-001", status = "ACTIVE", subStatus = null, txHash = null),
            )

        // when
        handler.handle(payload)

        // then
        verify(exactly = 0) { transactionStatusHandler.handleStatusUpdate(any(), any(), any(), any()) }
    }

    @Test
    fun `should return early when webhook data is null`() {
        // given
        val payload =
            WebhookPayload(
                type = "TRANSACTION_STATUS_UPDATED",
                tenantId = "t1",
                timestamp = 1700000000000,
                createdAt = 1700000000000,
                data = null,
            )

        // when
        handler.handle(payload)

        // then
        verify(exactly = 0) { transactionStatusHandler.handleStatusUpdate(any(), any(), any(), any()) }
    }

    @Test
    fun `should audit every webhook call`() {
        // given
        val auditSlot = slot<AuditLog>()
        every { auditLogRepository.save(capture(auditSlot)) } answers { firstArg() }
        val payload =
            WebhookPayload(
                type = "TRANSACTION_STATUS_UPDATED",
                tenantId = "t1",
                timestamp = 1700000000000,
                createdAt = 1700000000000,
                data = WebhookTransactionData(id = "fb-tx-audit", status = "BROADCASTING", subStatus = null, txHash = null),
            )

        // when
        handler.handle(payload)

        // then
        val saved = auditSlot.captured
        assertThat(saved.operation).isEqualTo(AuditOperation.WEBHOOK_RECEIVED)
        assertThat(saved.actor).isEqualTo("fireblocks")
        assertThat(saved.resourceId).isEqualTo("fb-tx-audit")
    }

    @Test
    fun `should audit with resourceId unknown when data is null`() {
        // given
        val auditSlot = slot<AuditLog>()
        every { auditLogRepository.save(capture(auditSlot)) } answers { firstArg() }
        val payload =
            WebhookPayload(
                type = "TRANSACTION_STATUS_UPDATED",
                tenantId = "t1",
                timestamp = 1700000000000,
                createdAt = 1700000000000,
                data = null,
            )

        // when
        handler.handle(payload)

        // then
        val saved = auditSlot.captured
        assertThat(saved.resourceId).isEqualTo("unknown")
    }
}
