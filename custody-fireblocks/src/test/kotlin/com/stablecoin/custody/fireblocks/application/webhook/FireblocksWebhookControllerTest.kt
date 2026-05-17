package com.stablecoin.custody.fireblocks.application.webhook

import com.stablecoin.custody.fireblocks.domain.audit.AuditLog
import com.stablecoin.custody.fireblocks.domain.audit.AuditLogRepository
import com.stablecoin.custody.fireblocks.domain.audit.AuditOperation
import com.stablecoin.custody.fireblocks.domain.transaction.TransactionStatusHandler
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders

class FireblocksWebhookControllerTest {
    private val transactionStatusHandler: TransactionStatusHandler = mockk(relaxed = true)
    private val auditLogRepository: AuditLogRepository = mockk()
    private val controller = FireblocksWebhookController(transactionStatusHandler, auditLogRepository)
    private lateinit var mockMvc: MockMvc

    @BeforeEach
    fun setUp() {
        every { auditLogRepository.save(any()) } answers { firstArg() }
        mockMvc =
            MockMvcBuilders
                .standaloneSetup(controller)
                .setMessageConverters(JacksonJsonHttpMessageConverter())
                .build()
    }

    @Test
    fun `should process TRANSACTION_STATUS_UPDATED webhook`() {
        // when / then
        mockMvc
            .perform(
                post("/api/v1/webhooks/fireblocks")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """{"type":"TRANSACTION_STATUS_UPDATED","tenantId":"t1","timestamp":1700000000000,"createdAt":1700000000000,"data":{"id":"fb-tx-001","status":"COMPLETED","subStatus":"CONFIRMED","txHash":"0xhash123"}}""",
                    ),
            ).andExpect(status().isOk)

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
    fun `should return 200 for unknown webhook type`() {
        // when / then
        mockMvc
            .perform(
                post("/api/v1/webhooks/fireblocks")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """{"type":"VAULT_CREATED","tenantId":"t1","timestamp":1700000000000,"createdAt":1700000000000,"data":{"id":"v-001","status":"ACTIVE","subStatus":null,"txHash":null}}""",
                    ),
            ).andExpect(status().isOk)

        verify(exactly = 0) { transactionStatusHandler.handleStatusUpdate(any(), any(), any(), any()) }
    }

    @Test
    fun `should return 200 when webhook data is null`() {
        // when / then
        mockMvc
            .perform(
                post("/api/v1/webhooks/fireblocks")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """{"type":"TRANSACTION_STATUS_UPDATED","tenantId":"t1","timestamp":1700000000000,"createdAt":1700000000000,"data":null}""",
                    ),
            ).andExpect(status().isOk)

        verify(exactly = 0) { transactionStatusHandler.handleStatusUpdate(any(), any(), any(), any()) }
    }

    @Test
    fun `should audit every webhook call`() {
        // given
        val auditSlot = slot<AuditLog>()
        every { auditLogRepository.save(capture(auditSlot)) } answers { firstArg() }

        // when
        mockMvc
            .perform(
                post("/api/v1/webhooks/fireblocks")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """{"type":"TRANSACTION_STATUS_UPDATED","tenantId":"t1","timestamp":1700000000000,"createdAt":1700000000000,"data":{"id":"fb-tx-audit","status":"BROADCASTING","subStatus":null,"txHash":null}}""",
                    ),
            ).andExpect(status().isOk)

        // then
        verify { auditLogRepository.save(any()) }
        val saved = auditSlot.captured
        assert(saved.operation == AuditOperation.WEBHOOK_RECEIVED)
        assert(saved.actor == "fireblocks")
        assert(saved.resourceId == "fb-tx-audit")
    }

    @Test
    fun `should delegate to TransactionStatusHandler`() {
        // when
        mockMvc
            .perform(
                post("/api/v1/webhooks/fireblocks")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """{"type":"TRANSACTION_STATUS_UPDATED","tenantId":"t1","timestamp":1700000000000,"createdAt":1700000000000,"data":{"id":"fb-delegate","status":"PENDING_SIGNATURE","subStatus":"PENDING_3RD_PARTY","txHash":null}}""",
                    ),
            ).andExpect(status().isOk)

        // then
        verify {
            transactionStatusHandler.handleStatusUpdate(
                fireblocksTxId = "fb-delegate",
                fireblocksStatus = "PENDING_SIGNATURE",
                subStatus = "PENDING_3RD_PARTY",
                txHash = null,
            )
        }
    }
}
