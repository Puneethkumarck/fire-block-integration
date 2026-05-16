package com.stablecoin.custody.fireblocks.domain.transaction

import com.stablecoin.custody.fireblocks.AbstractIntegrationTest
import com.stablecoin.custody.fireblocks.domain.audit.AuditLogRepository
import com.stablecoin.custody.fireblocks.domain.audit.AuditOperation
import com.stablecoin.custody.fireblocks.domain.audit.AuditStatus
import com.stablecoin.custody.fireblocks.test.fixtures.aTransaction
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class TransactionStatusHandlerIntegrationTest : AbstractIntegrationTest() {
    @Autowired
    private lateinit var handler: TransactionStatusHandler

    @Autowired
    private lateinit var transactionRepository: TransactionRepository

    @Autowired
    private lateinit var auditLogRepository: AuditLogRepository

    @Test
    fun `should update status end-to-end`() {
        // given
        val transaction =
            aTransaction(
                status = TransactionStatus.SUBMITTED,
                fireblocksTransactionId = "fb-tx-status-001",
            )
        transactionRepository.save(transaction)

        // when
        handler.handleStatusUpdate("fb-tx-status-001", "BROADCASTING", null, null)

        // then
        val updated = transactionRepository.findByFireblocksTransactionId("fb-tx-status-001")
        assertThat(updated).isNotNull
        assertThat(updated!!.status).isEqualTo(TransactionStatus.PROCESSING)

        val auditLogs = auditLogRepository.findByResourceId(updated.id.value.toString())
        assertThat(auditLogs).anyMatch {
            it.operation == AuditOperation.TRANSACTION_STATUS_UPDATED && it.status == AuditStatus.SUCCESS
        }
    }

    @Test
    fun `should handle concurrent status updates with pessimistic lock`() {
        // given
        val transaction =
            aTransaction(
                status = TransactionStatus.SUBMITTED,
                fireblocksTransactionId = "fb-tx-concurrent-001",
            )
        transactionRepository.save(transaction)

        // when
        handler.handleStatusUpdate("fb-tx-concurrent-001", "BROADCASTING", null, null)

        // then
        val updated = transactionRepository.findByFireblocksTransactionId("fb-tx-concurrent-001")
        assertThat(updated).isNotNull
        assertThat(updated!!.status).isEqualTo(TransactionStatus.PROCESSING)
    }

    @Test
    fun `should handle webhook for unknown transaction`() {
        // when
        handler.handleStatusUpdate("fb-tx-nonexistent-001", "SUBMITTED", null, null)

        // then
        val result = transactionRepository.findByFireblocksTransactionId("fb-tx-nonexistent-001")
        assertThat(result).isNull()
    }
}
