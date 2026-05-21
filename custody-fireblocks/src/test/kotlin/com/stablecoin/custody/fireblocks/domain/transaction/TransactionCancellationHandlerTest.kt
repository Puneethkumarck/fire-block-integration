package com.stablecoin.custody.fireblocks.domain.transaction

import com.stablecoin.custody.fireblocks.domain.audit.AuditLogRepository
import com.stablecoin.custody.fireblocks.domain.audit.AuditOperation
import com.stablecoin.custody.fireblocks.domain.audit.AuditStatus
import com.stablecoin.custody.fireblocks.domain.exception.InvalidTransactionStateException
import com.stablecoin.custody.fireblocks.domain.exception.TransactionAlreadyTerminalException
import com.stablecoin.custody.fireblocks.domain.exception.TransactionNotCancellableException
import com.stablecoin.custody.fireblocks.domain.exception.TransactionNotFoundException
import com.stablecoin.custody.fireblocks.domain.port.FireblocksTransactionPort
import com.stablecoin.custody.fireblocks.test.fixtures.aTransaction
import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.util.UUID

@ExtendWith(MockKExtension::class)
class TransactionCancellationHandlerTest {
    private val transactionRepository: TransactionRepository = mockk()
    private val fireblocksTransactionPort: FireblocksTransactionPort = mockk()
    private val auditLogRepository: AuditLogRepository = mockk()

    private val handler =
        TransactionCancellationHandler(
            transactionRepository,
            fireblocksTransactionPort,
            auditLogRepository,
        )

    @Test
    fun `should cancel transaction and audit success`() {
        // given
        val transaction =
            aTransaction(
                status = TransactionStatus.PROCESSING,
                fireblocksTransactionId = "fb-tx-001",
            )
        every { transactionRepository.findById(transaction.id) } returns transaction
        every { fireblocksTransactionPort.cancelTransaction("fb-tx-001") } returns true
        every { auditLogRepository.save(any()) } returnsArgument 0

        // when
        val result = handler.handle(transaction.id)

        // then
        assertThat(result).usingRecursiveComparison().isEqualTo(transaction)
        verify {
            auditLogRepository.save(
                match {
                    it.operation == AuditOperation.TRANSACTION_CANCELLATION_REQUESTED &&
                        it.status == AuditStatus.SUCCESS
                },
            )
        }
    }

    @Test
    fun `should throw TransactionNotFoundException when transaction does not exist`() {
        // given
        val transactionId = TransactionId(UUID.randomUUID())
        every { transactionRepository.findById(transactionId) } returns null

        // when
        val thrown = assertThatThrownBy { handler.handle(transactionId) }

        // then
        thrown.isInstanceOf(TransactionNotFoundException::class.java)
    }

    @Test
    fun `should throw TransactionAlreadyTerminalException for CONFIRMED transaction`() {
        // given
        val transaction =
            aTransaction(
                status = TransactionStatus.CONFIRMED,
                fireblocksTransactionId = "fb-tx-002",
            )
        every { transactionRepository.findById(transaction.id) } returns transaction

        // when
        val thrown = assertThatThrownBy { handler.handle(transaction.id) }

        // then
        thrown.isInstanceOf(TransactionAlreadyTerminalException::class.java)
    }

    @Test
    fun `should throw TransactionAlreadyTerminalException for FAILED transaction`() {
        // given
        val transaction =
            aTransaction(
                status = TransactionStatus.FAILED,
                fireblocksTransactionId = "fb-tx-003",
            )
        every { transactionRepository.findById(transaction.id) } returns transaction

        // when
        val thrown = assertThatThrownBy { handler.handle(transaction.id) }

        // then
        thrown.isInstanceOf(TransactionAlreadyTerminalException::class.java)
    }

    @Test
    fun `should throw InvalidTransactionStateException for CREATED transaction without Fireblocks ID`() {
        // given
        val transaction =
            aTransaction(
                status = TransactionStatus.CREATED,
                fireblocksTransactionId = null,
            )
        every { transactionRepository.findById(transaction.id) } returns transaction

        // when
        val thrown = assertThatThrownBy { handler.handle(transaction.id) }

        // then
        thrown.isInstanceOf(InvalidTransactionStateException::class.java)
    }

    @Test
    fun `should throw TransactionNotCancellableException when Fireblocks returns false`() {
        // given
        val transaction =
            aTransaction(
                status = TransactionStatus.PROCESSING,
                fireblocksTransactionId = "fb-tx-004",
            )
        every { transactionRepository.findById(transaction.id) } returns transaction
        every { fireblocksTransactionPort.cancelTransaction("fb-tx-004") } returns false
        every { auditLogRepository.save(any()) } returnsArgument 0

        // when
        val thrown = assertThatThrownBy { handler.handle(transaction.id) }

        // then
        thrown.isInstanceOf(TransactionNotCancellableException::class.java)
        verify {
            auditLogRepository.save(
                match {
                    it.operation == AuditOperation.TRANSACTION_CANCELLATION_REJECTED &&
                        it.status == AuditStatus.FAILURE
                },
            )
        }
    }
}
