package com.stablecoin.custody.fireblocks.domain.transaction

import com.stablecoin.custody.fireblocks.domain.audit.AuditLogRepository
import com.stablecoin.custody.fireblocks.domain.audit.AuditOperation
import com.stablecoin.custody.fireblocks.domain.audit.AuditStatus
import com.stablecoin.custody.fireblocks.domain.event.TransactionStatusChangedEvent
import com.stablecoin.custody.fireblocks.domain.exception.InvalidTransactionStateException
import com.stablecoin.custody.fireblocks.domain.port.EventPublisher
import com.stablecoin.custody.fireblocks.domain.shared.StateMachine
import com.stablecoin.custody.fireblocks.test.fixtures.aTransaction
import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class TransactionStatusHandlerTest {
    private val transactionRepository: TransactionRepository = mockk()
    private val eventPublisher: EventPublisher<TransactionStatusChangedEvent> = mockk()
    private val auditLogRepository: AuditLogRepository = mockk()
    private val stateMachine: StateMachine<TransactionStatus, Transaction> = mockk()

    private val handler =
        TransactionStatusHandler(
            transactionRepository,
            eventPublisher,
            auditLogRepository,
            stateMachine,
        )

    @Test
    fun `should update transaction status from SUBMITTED to PROCESSING`() {
        // given
        val transaction = aTransaction(status = TransactionStatus.SUBMITTED, fireblocksTransactionId = "fb-tx-001")
        every { transactionRepository.findByFireblocksTransactionId("fb-tx-001") } returns transaction
        every { stateMachine.transition(transaction, TransactionStatus.PROCESSING) } returns TransactionStatus.PROCESSING
        every { transactionRepository.findByIdForUpdate(transaction.id) } returns transaction
        every { transactionRepository.save(any()) } returnsArgument 0
        every { eventPublisher.publish(any()) } just runs
        every { auditLogRepository.save(any()) } returnsArgument 0

        // when
        handler.handleStatusUpdate("fb-tx-001", "SUBMITTED", null, null)

        // then
        verify {
            transactionRepository.save(
                match { it.status == TransactionStatus.PROCESSING },
            )
        }
    }

    @Test
    fun `should update transaction status from PROCESSING to CONFIRMING`() {
        // given
        val transaction = aTransaction(status = TransactionStatus.PROCESSING, fireblocksTransactionId = "fb-tx-002")
        every { transactionRepository.findByFireblocksTransactionId("fb-tx-002") } returns transaction
        every { stateMachine.transition(transaction, TransactionStatus.CONFIRMING) } returns TransactionStatus.CONFIRMING
        every { transactionRepository.findByIdForUpdate(transaction.id) } returns transaction
        every { transactionRepository.save(any()) } returnsArgument 0
        every { eventPublisher.publish(any()) } just runs
        every { auditLogRepository.save(any()) } returnsArgument 0

        // when
        handler.handleStatusUpdate("fb-tx-002", "CONFIRMING", null, "0xhash123")

        // then
        verify {
            transactionRepository.save(
                match { it.status == TransactionStatus.CONFIRMING && it.txHash == "0xhash123" },
            )
        }
    }

    @Test
    fun `should update transaction status from CONFIRMING to CONFIRMED`() {
        // given
        val transaction = aTransaction(status = TransactionStatus.CONFIRMING, fireblocksTransactionId = "fb-tx-003")
        every { transactionRepository.findByFireblocksTransactionId("fb-tx-003") } returns transaction
        every { stateMachine.transition(transaction, TransactionStatus.CONFIRMED) } returns TransactionStatus.CONFIRMED
        every { transactionRepository.findByIdForUpdate(transaction.id) } returns transaction
        every { transactionRepository.save(any()) } returnsArgument 0
        every { eventPublisher.publish(any()) } just runs
        every { auditLogRepository.save(any()) } returnsArgument 0

        // when
        handler.handleStatusUpdate("fb-tx-003", "COMPLETED", null, "0xfinalhash")

        // then
        verify {
            transactionRepository.save(
                match { it.status == TransactionStatus.CONFIRMED && it.txHash == "0xfinalhash" },
            )
        }
    }

    @Test
    fun `should update transaction status to FAILED`() {
        // given
        val transaction = aTransaction(status = TransactionStatus.PROCESSING, fireblocksTransactionId = "fb-tx-004")
        every { transactionRepository.findByFireblocksTransactionId("fb-tx-004") } returns transaction
        every { stateMachine.transition(transaction, TransactionStatus.FAILED) } returns TransactionStatus.FAILED
        every { transactionRepository.findByIdForUpdate(transaction.id) } returns transaction
        every { transactionRepository.save(any()) } returnsArgument 0
        every { eventPublisher.publish(any()) } just runs
        every { auditLogRepository.save(any()) } returnsArgument 0

        // when
        handler.handleStatusUpdate("fb-tx-004", "FAILED", "INSUFFICIENT_FUNDS", null)

        // then
        verify {
            transactionRepository.save(
                match { it.status == TransactionStatus.FAILED },
            )
        }
    }

    @Test
    fun `should skip update when already in same status`() {
        // given
        val transaction = aTransaction(status = TransactionStatus.PROCESSING, fireblocksTransactionId = "fb-tx-005")
        every { transactionRepository.findByFireblocksTransactionId("fb-tx-005") } returns transaction

        // when
        handler.handleStatusUpdate("fb-tx-005", "BROADCASTING", null, null)

        // then
        verify(exactly = 0) { transactionRepository.save(any()) }
        verify(exactly = 0) { eventPublisher.publish(any()) }
    }

    @Test
    fun `should skip update when transaction is already terminal`() {
        // given
        val transaction = aTransaction(status = TransactionStatus.CONFIRMED, fireblocksTransactionId = "fb-tx-006")
        every { transactionRepository.findByFireblocksTransactionId("fb-tx-006") } returns transaction

        // when
        handler.handleStatusUpdate("fb-tx-006", "FAILED", null, null)

        // then
        verify(exactly = 0) { transactionRepository.save(any()) }
        verify(exactly = 0) { eventPublisher.publish(any()) }
    }

    @Test
    fun `should log warn for unknown transaction`() {
        // given
        every { transactionRepository.findByFireblocksTransactionId("unknown-tx") } returns null

        // when
        handler.handleStatusUpdate("unknown-tx", "SUBMITTED", null, null)

        // then
        verify(exactly = 0) { transactionRepository.save(any()) }
        verify(exactly = 0) { eventPublisher.publish(any()) }
    }

    @Test
    fun `should acquire pessimistic lock before update`() {
        // given
        val transaction = aTransaction(status = TransactionStatus.SUBMITTED, fireblocksTransactionId = "fb-tx-lock")
        every { transactionRepository.findByFireblocksTransactionId("fb-tx-lock") } returns transaction
        every { stateMachine.transition(transaction, TransactionStatus.PROCESSING) } returns TransactionStatus.PROCESSING
        every { transactionRepository.findByIdForUpdate(transaction.id) } returns transaction
        every { transactionRepository.save(any()) } returnsArgument 0
        every { eventPublisher.publish(any()) } just runs
        every { auditLogRepository.save(any()) } returnsArgument 0

        // when
        handler.handleStatusUpdate("fb-tx-lock", "SUBMITTED", null, null)

        // then
        verify { transactionRepository.findByIdForUpdate(transaction.id) }
    }

    @Test
    fun `should publish event on status change`() {
        // given
        val transaction = aTransaction(status = TransactionStatus.SUBMITTED, fireblocksTransactionId = "fb-tx-evt")
        every { transactionRepository.findByFireblocksTransactionId("fb-tx-evt") } returns transaction
        every { stateMachine.transition(transaction, TransactionStatus.PROCESSING) } returns TransactionStatus.PROCESSING
        every { transactionRepository.findByIdForUpdate(transaction.id) } returns transaction
        every { transactionRepository.save(any()) } returnsArgument 0
        every { eventPublisher.publish(any()) } just runs
        every { auditLogRepository.save(any()) } returnsArgument 0

        // when
        handler.handleStatusUpdate("fb-tx-evt", "SUBMITTED", null, null)

        // then
        verify {
            eventPublisher.publish(
                match {
                    it.previousStatus == "SUBMITTED" &&
                        it.newStatus == "PROCESSING" &&
                        it.externalTxId == transaction.externalTxId
                },
            )
        }
    }

    @Test
    fun `should save audit log on status change`() {
        // given
        val transaction = aTransaction(status = TransactionStatus.SUBMITTED, fireblocksTransactionId = "fb-tx-aud")
        every { transactionRepository.findByFireblocksTransactionId("fb-tx-aud") } returns transaction
        every { stateMachine.transition(transaction, TransactionStatus.PROCESSING) } returns TransactionStatus.PROCESSING
        every { transactionRepository.findByIdForUpdate(transaction.id) } returns transaction
        every { transactionRepository.save(any()) } returnsArgument 0
        every { eventPublisher.publish(any()) } just runs
        every { auditLogRepository.save(any()) } returnsArgument 0

        // when
        handler.handleStatusUpdate("fb-tx-aud", "SUBMITTED", null, null)

        // then
        verify {
            auditLogRepository.save(
                match {
                    it.operation == AuditOperation.TRANSACTION_STATUS_UPDATED &&
                        it.status == AuditStatus.SUCCESS
                },
            )
        }
    }

    @Test
    fun `should reject invalid state transition`() {
        // given
        val transaction = aTransaction(status = TransactionStatus.SUBMITTED, fireblocksTransactionId = "fb-tx-inv")
        every { transactionRepository.findByFireblocksTransactionId("fb-tx-inv") } returns transaction
        every { stateMachine.transition(transaction, TransactionStatus.CONFIRMED) } throws
            InvalidTransactionStateException(transaction.id.value.toString(), "SUBMITTED", "CONFIRMED")

        // when/then
        assertThatThrownBy { handler.handleStatusUpdate("fb-tx-inv", "COMPLETED", null, null) }
            .isInstanceOf(InvalidTransactionStateException::class.java)
    }
}
