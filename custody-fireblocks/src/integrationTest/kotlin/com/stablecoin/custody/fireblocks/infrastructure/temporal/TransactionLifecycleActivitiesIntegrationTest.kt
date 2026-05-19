package com.stablecoin.custody.fireblocks.infrastructure.temporal

import com.stablecoin.custody.fireblocks.AbstractIntegrationTest
import com.stablecoin.custody.fireblocks.domain.audit.AuditLogRepository
import com.stablecoin.custody.fireblocks.domain.audit.AuditOperation
import com.stablecoin.custody.fireblocks.domain.transaction.TransactionRepository
import com.stablecoin.custody.fireblocks.domain.transaction.TransactionStatus
import com.stablecoin.custody.fireblocks.test.fixtures.aStartTransactionRequest
import jakarta.persistence.EntityManager
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

@Transactional
class TransactionLifecycleActivitiesIntegrationTest : AbstractIntegrationTest() {
    @Autowired
    private lateinit var activities: TransactionLifecycleActivitiesImpl

    @Autowired
    private lateinit var transactionRepository: TransactionRepository

    @Autowired
    private lateinit var auditLogRepository: AuditLogRepository

    @Autowired
    private lateinit var entityManager: EntityManager

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @Test
    fun `should create transaction and persist to database`() {
        // given
        val request = aStartTransactionRequest(externalTxId = "integ-ext-tx-001")

        // when
        val result = activities.createTransaction(request)
        entityManager.flush()
        entityManager.clear()

        // then
        val saved = transactionRepository.findByExternalTxId("integ-ext-tx-001")
        assertThat(saved).isNotNull
        assertThat(saved!!.id.value.toString()).isEqualTo(result.transactionId)
        assertThat(saved.status).isEqualTo(TransactionStatus.CREATED)
        assertThat(saved.amount).isEqualByComparingTo(request.amount)
    }

    @Test
    fun `should create transaction and publish outbox event`() {
        // given
        val request = aStartTransactionRequest(externalTxId = "integ-ext-tx-outbox")

        // when
        activities.createTransaction(request)
        entityManager.flush()

        // then
        val count =
            jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM custody_outbox_record WHERE record_type = ?",
                Long::class.java,
                "com.stablecoin.custody.fireblocks.domain.event.TransactionStatusChangedEvent",
            )
        assertThat(count).isGreaterThan(0)
    }

    @Test
    fun `should create transaction and write audit log`() {
        // given
        val request = aStartTransactionRequest(externalTxId = "integ-ext-tx-audit")

        // when
        val result = activities.createTransaction(request)
        entityManager.flush()
        entityManager.clear()

        // then
        val auditLogs = auditLogRepository.findByResourceId(result.transactionId)
        assertThat(auditLogs).isNotEmpty
        assertThat(auditLogs[0].operation).isEqualTo(AuditOperation.TRANSACTION_CREATED)
        assertThat(auditLogs[0].actor).isEqualTo("temporal-activity")
    }

    @Test
    fun `should return existing transaction for duplicate externalTxId`() {
        // given
        val request = aStartTransactionRequest(externalTxId = "integ-ext-tx-dup")
        val firstResult = activities.createTransaction(request)
        entityManager.flush()
        entityManager.clear()

        // when
        val secondResult = activities.createTransaction(request)

        // then
        assertThat(secondResult.transactionId).isEqualTo(firstResult.transactionId)
    }

    @Test
    fun `should record submission and transition to SUBMITTED`() {
        // given
        val request = aStartTransactionRequest(externalTxId = "integ-ext-tx-submit")
        val createResult = activities.createTransaction(request)
        entityManager.flush()
        entityManager.clear()

        // when
        activities.recordSubmission(createResult.transactionId, "fb-tx-integ-001")
        entityManager.flush()
        entityManager.clear()

        // then
        val updated = transactionRepository.findByExternalTxId("integ-ext-tx-submit")
        assertThat(updated).isNotNull
        assertThat(updated!!.status).isEqualTo(TransactionStatus.SUBMITTED)
        assertThat(updated.fireblocksTransactionId).isEqualTo("fb-tx-integ-001")
    }

    @Test
    fun `should record status change and transition to PROCESSING`() {
        // given
        val request = aStartTransactionRequest(externalTxId = "integ-ext-tx-status")
        val createResult = activities.createTransaction(request)
        activities.recordSubmission(createResult.transactionId, "fb-tx-integ-002")
        entityManager.flush()
        entityManager.clear()

        // when
        activities.recordStatusChange(
            createResult.transactionId,
            "PROCESSING",
            "PENDING_SIGNATURE",
            "PENDING_SIGNER_AUTHORIZATION",
            null,
        )
        entityManager.flush()
        entityManager.clear()

        // then
        val updated = transactionRepository.findByExternalTxId("integ-ext-tx-status")
        assertThat(updated).isNotNull
        assertThat(updated!!.status).isEqualTo(TransactionStatus.PROCESSING)
        assertThat(updated.fireblocksStatus).isEqualTo("PENDING_SIGNATURE")
        assertThat(updated.fireblocksSubStatus).isEqualTo("PENDING_SIGNER_AUTHORIZATION")
    }

    @Test
    fun `should complete transaction with CONFIRMED status`() {
        // given
        val request = aStartTransactionRequest(externalTxId = "integ-ext-tx-complete")
        val createResult = activities.createTransaction(request)
        activities.recordSubmission(createResult.transactionId, "fb-tx-integ-003")
        activities.recordStatusChange(createResult.transactionId, "CONFIRMING", "CONFIRMING", null, null)
        entityManager.flush()
        entityManager.clear()

        // when
        activities.completeTransaction(
            createResult.transactionId,
            "CONFIRMED",
            "COMPLETED",
            null,
            "0xhash-integ-001",
        )
        entityManager.flush()
        entityManager.clear()

        // then
        val updated = transactionRepository.findByExternalTxId("integ-ext-tx-complete")
        assertThat(updated).isNotNull
        assertThat(updated!!.status).isEqualTo(TransactionStatus.CONFIRMED)
        assertThat(updated.txHash).isEqualTo("0xhash-integ-001")
    }

    @Test
    fun `should complete transaction with FAILED status`() {
        // given
        val request = aStartTransactionRequest(externalTxId = "integ-ext-tx-fail")
        val createResult = activities.createTransaction(request)
        activities.recordSubmission(createResult.transactionId, "fb-tx-integ-004")
        entityManager.flush()
        entityManager.clear()

        // when
        activities.completeTransaction(
            createResult.transactionId,
            "FAILED",
            "FAILED",
            "INSUFFICIENT_FUNDS",
            null,
        )
        entityManager.flush()
        entityManager.clear()

        // then
        val updated = transactionRepository.findByExternalTxId("integ-ext-tx-fail")
        assertThat(updated).isNotNull
        assertThat(updated!!.status).isEqualTo(TransactionStatus.FAILED)
        assertThat(updated.fireblocksSubStatus).isEqualTo("INSUFFICIENT_FUNDS")
    }

    @Test
    fun `should preserve BigDecimal precision through full lifecycle`() {
        // given
        val preciseAmount = BigDecimal("123456789.123456789012345678")
        val request =
            aStartTransactionRequest(
                externalTxId = "integ-ext-tx-precision",
                amount = preciseAmount,
            )

        // when
        activities.createTransaction(request)
        entityManager.flush()
        entityManager.clear()

        // then
        val saved = transactionRepository.findByExternalTxId("integ-ext-tx-precision")
        assertThat(saved!!.amount).isEqualByComparingTo(preciseAmount)
    }

    @Test
    fun `should throw when recording submission for non-existent transaction`() {
        // given
        val fakeTransactionId =
            java.util.UUID
                .randomUUID()
                .toString()

        // when / then
        assertThatThrownBy { activities.recordSubmission(fakeTransactionId, "fb-tx-xxx") }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("Transaction not found")
    }
}
