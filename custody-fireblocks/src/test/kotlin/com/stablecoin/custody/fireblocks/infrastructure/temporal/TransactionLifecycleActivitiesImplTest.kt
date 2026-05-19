package com.stablecoin.custody.fireblocks.infrastructure.temporal

import com.stablecoin.custody.fireblocks.domain.audit.AuditLog
import com.stablecoin.custody.fireblocks.domain.audit.AuditLogRepository
import com.stablecoin.custody.fireblocks.domain.audit.AuditOperation
import com.stablecoin.custody.fireblocks.domain.audit.AuditStatus
import com.stablecoin.custody.fireblocks.domain.event.TransactionStatusChangedEvent
import com.stablecoin.custody.fireblocks.domain.port.EventPublisher
import com.stablecoin.custody.fireblocks.domain.port.FireblocksTransactionPort
import com.stablecoin.custody.fireblocks.domain.transaction.TransactionRepository
import com.stablecoin.custody.fireblocks.domain.transaction.TransactionStatus
import com.stablecoin.custody.fireblocks.infrastructure.temporal.dto.PollStatusResult
import com.stablecoin.custody.fireblocks.infrastructure.temporal.dto.ReleaseFundsActivityCommand
import com.stablecoin.custody.fireblocks.infrastructure.temporal.dto.ReserveFundsActivityCommand
import com.stablecoin.custody.fireblocks.infrastructure.temporal.dto.SubmitResult
import com.stablecoin.custody.fireblocks.test.fixtures.aFireblocksSubmitActivityCommand
import com.stablecoin.custody.fireblocks.test.fixtures.aStartTransactionRequest
import com.stablecoin.custody.fireblocks.test.fixtures.aTransaction
import com.stablecoin.custody.fireblocks.test.fixtures.aTransactionResult
import io.mockk.every
import io.mockk.just
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.util.UUID

class TransactionLifecycleActivitiesImplTest {
    private val transactionRepository = io.mockk.mockk<TransactionRepository>()
    private val fireblocksTransactionPort = io.mockk.mockk<FireblocksTransactionPort>()
    private val eventPublisher = io.mockk.mockk<EventPublisher<TransactionStatusChangedEvent>>()
    private val auditLogRepository = io.mockk.mockk<AuditLogRepository>()

    private val activities =
        TransactionLifecycleActivitiesImpl(
            transactionRepository = transactionRepository,
            fireblocksTransactionPort = fireblocksTransactionPort,
            eventPublisher = eventPublisher,
            auditLogRepository = auditLogRepository,
        )

    @Test
    fun `should create transaction and return result`() {
        // given
        val request = aStartTransactionRequest(externalTxId = "ext-tx-001")
        val transactionSlot = slot<com.stablecoin.custody.fireblocks.domain.transaction.Transaction>()

        every { transactionRepository.findByExternalTxId("ext-tx-001") } returns null
        every { transactionRepository.save(capture(transactionSlot)) } answers { transactionSlot.captured }
        every { eventPublisher.publish(any()) } just runs
        every { auditLogRepository.save(any()) } answers { firstArg() }

        // when
        val result = activities.createTransaction(request)

        // then
        assertThat(result.transactionId).isNotBlank()
        verify { transactionRepository.save(any()) }
        verify { eventPublisher.publish(any()) }
        verify { auditLogRepository.save(any()) }
    }

    @Test
    fun `should return existing transaction when duplicate externalTxId`() {
        // given
        val request = aStartTransactionRequest(externalTxId = "ext-tx-dup")
        val existing = aTransaction(externalTxId = "ext-tx-dup")

        every { transactionRepository.findByExternalTxId("ext-tx-dup") } returns existing

        // when
        val result = activities.createTransaction(request)

        // then
        assertThat(result.transactionId).isEqualTo(existing.id.value.toString())
        verify(exactly = 0) { transactionRepository.save(any()) }
    }

    @Test
    fun `should record submission and publish event`() {
        // given
        val transaction = aTransaction(status = TransactionStatus.CREATED)
        val transactionId = transaction.id.value.toString()
        val fireblocksTransactionId = "fb-tx-123"

        every { transactionRepository.findById(transaction.id) } returns transaction
        every { transactionRepository.save(any()) } answers { firstArg() }
        every { eventPublisher.publish(any()) } just runs
        every { auditLogRepository.save(any()) } answers { firstArg() }

        // when
        activities.recordSubmission(transactionId, fireblocksTransactionId)

        // then
        val savedSlot = slot<com.stablecoin.custody.fireblocks.domain.transaction.Transaction>()
        verify { transactionRepository.save(capture(savedSlot)) }
        val expectedTransaction = transaction.markSubmitted(fireblocksTransactionId)
        assertThat(savedSlot.captured)
            .usingRecursiveComparison()
            .ignoringFields("updatedAt")
            .isEqualTo(expectedTransaction)

        val eventSlot = slot<TransactionStatusChangedEvent>()
        verify { eventPublisher.publish(capture(eventSlot)) }
        val expectedEvent =
            TransactionStatusChangedEvent(
                transactionId = transaction.id.value,
                externalTxId = transaction.externalTxId,
                previousStatus = "CREATED",
                newStatus = "SUBMITTED",
                fireblocksStatus = null,
                subStatus = null,
                txHash = null,
                occurredAt = eventSlot.captured.occurredAt,
            )
        assertThat(eventSlot.captured)
            .usingRecursiveComparison()
            .isEqualTo(expectedEvent)
    }

    @Test
    fun `should record status change for intermediate statuses`() {
        // given
        val transaction = aTransaction(status = TransactionStatus.SUBMITTED, fireblocksTransactionId = "fb-tx-123")
        val transactionId = transaction.id.value.toString()

        every { transactionRepository.findById(transaction.id) } returns transaction
        every { transactionRepository.save(any()) } answers { firstArg() }
        every { eventPublisher.publish(any()) } just runs
        every { auditLogRepository.save(any()) } answers { firstArg() }

        // when
        activities.recordStatusChange(transactionId, "PROCESSING", "PENDING_SIGNATURE", null, null)

        // then
        val savedSlot = slot<com.stablecoin.custody.fireblocks.domain.transaction.Transaction>()
        verify { transactionRepository.save(capture(savedSlot)) }
        val expectedTransaction = transaction.updateStatus(TransactionStatus.PROCESSING, "PENDING_SIGNATURE", null, null)
        assertThat(savedSlot.captured)
            .usingRecursiveComparison()
            .ignoringFields("updatedAt")
            .isEqualTo(expectedTransaction)

        val auditSlot = slot<AuditLog>()
        verify { auditLogRepository.save(capture(auditSlot)) }
        val expectedAudit =
            AuditLog.create(
                operation = AuditOperation.TRANSACTION_STATUS_UPDATED,
                actor = "temporal-activity",
                resourceId = transactionId,
                status = AuditStatus.SUCCESS,
                details =
                    mapOf(
                        "previousStatus" to "SUBMITTED",
                        "newStatus" to "PROCESSING",
                        "fireblocksStatus" to "PENDING_SIGNATURE",
                    ),
            )
        assertThat(auditSlot.captured)
            .usingRecursiveComparison()
            .ignoringFields("id", "timestamp")
            .isEqualTo(expectedAudit)
    }

    @Test
    fun `should complete transaction with terminal status`() {
        // given
        val transaction = aTransaction(status = TransactionStatus.CONFIRMING, fireblocksTransactionId = "fb-tx-123")
        val transactionId = transaction.id.value.toString()

        every { transactionRepository.findById(transaction.id) } returns transaction
        every { transactionRepository.save(any()) } answers { firstArg() }
        every { eventPublisher.publish(any()) } just runs
        every { auditLogRepository.save(any()) } answers { firstArg() }

        // when
        activities.completeTransaction(transactionId, "CONFIRMED", "COMPLETED", null, "0xhash123")

        // then
        val savedSlot = slot<com.stablecoin.custody.fireblocks.domain.transaction.Transaction>()
        verify { transactionRepository.save(capture(savedSlot)) }
        val expectedTransaction = transaction.updateStatus(TransactionStatus.CONFIRMED, "COMPLETED", null, "0xhash123")
        assertThat(savedSlot.captured)
            .usingRecursiveComparison()
            .ignoringFields("updatedAt")
            .isEqualTo(expectedTransaction)
    }

    @Test
    fun `should submit to Fireblocks and return result`() {
        // given
        val command = aFireblocksSubmitActivityCommand(externalTxId = "ext-tx-001")
        val fireblocksResult = aTransactionResult(id = "fb-tx-456", status = "SUBMITTED")

        every { fireblocksTransactionPort.submitTransaction(any()) } returns fireblocksResult

        // when
        val result = activities.submitToFireblocks(command)

        // then
        val expected =
            SubmitResult(
                fireblocksTransactionId = "fb-tx-456",
                status = "SUBMITTED",
                subStatus = null,
                txHash = null,
            )
        assertThat(result).usingRecursiveComparison().isEqualTo(expected)
    }

    @Test
    fun `should fetch transaction status from Fireblocks`() {
        // given
        val fireblocksResult =
            aTransactionResult(
                id = "fb-tx-789",
                status = "CONFIRMING",
                subStatus = "PENDING_BLOCKCHAIN_CONFIRMATIONS",
                txHash = "0xhash",
            )

        every { fireblocksTransactionPort.getTransaction("fb-tx-789") } returns fireblocksResult

        // when
        val result = activities.fetchTransactionStatus("fb-tx-789")

        // then
        val expected =
            PollStatusResult(
                fireblocksStatus = "CONFIRMING",
                subStatus = "PENDING_BLOCKCHAIN_CONFIRMATIONS",
                txHash = "0xhash",
            )
        assertThat(result).usingRecursiveComparison().isEqualTo(expected)
    }

    @Test
    fun `should throw UnsupportedOperationException for reserveFunds`() {
        // given
        val command =
            ReserveFundsActivityCommand(
                vaultId = "vault-1",
                assetId = "BTC",
                amount = BigDecimal("1.0"),
            )

        // when / then
        assertThatThrownBy { activities.reserveFunds(command) }
            .isInstanceOf(UnsupportedOperationException::class.java)
            .hasMessage("Fund allocation not yet implemented")
    }

    @Test
    fun `should throw UnsupportedOperationException for releaseFunds`() {
        // given
        val command = ReleaseFundsActivityCommand(allocationId = UUID.randomUUID().toString())

        // when / then
        assertThatThrownBy { activities.releaseFunds(command) }
            .isInstanceOf(UnsupportedOperationException::class.java)
            .hasMessage("Fund allocation not yet implemented")
    }

    @Test
    fun `should throw IllegalArgumentException when transaction not found`() {
        // given
        val transactionId = UUID.randomUUID().toString()

        every { transactionRepository.findById(any()) } returns null

        // when / then
        assertThatThrownBy { activities.recordSubmission(transactionId, "fb-tx-123") }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("Transaction not found")
    }
}
