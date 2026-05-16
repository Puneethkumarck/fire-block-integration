package com.stablecoin.custody.fireblocks.domain.transaction

import com.stablecoin.custody.fireblocks.test.fixtures.aTransaction
import com.stablecoin.custody.fireblocks.test.fixtures.aTransactionStatusChangedEvent
import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class TransactionStatusUpdateHandlerTest {
    private val transactionRepository: TransactionRepository = mockk()
    private val handler = TransactionStatusUpdateHandler(transactionRepository)

    @Test
    fun `should update transaction status`() {
        // given
        val transaction = aTransaction(status = TransactionStatus.SUBMITTED)
        val event =
            aTransactionStatusChangedEvent(
                externalTxId = transaction.externalTxId,
                previousStatus = "SUBMITTED",
                newStatus = "PROCESSING",
                fireblocksStatus = "PENDING_SIGNATURE",
            )
        every { transactionRepository.findByExternalTxId(transaction.externalTxId) } returns transaction
        every { transactionRepository.save(match { it.externalTxId == transaction.externalTxId }) } returnsArgument 0

        // when
        handler.handle(event)

        // then
        verify {
            transactionRepository.save(
                match {
                    it.status == TransactionStatus.PROCESSING &&
                        it.fireblocksStatus == "PENDING_SIGNATURE"
                },
            )
        }
    }

    @Test
    fun `should be idempotent for duplicate events`() {
        // given
        val transaction = aTransaction(status = TransactionStatus.PROCESSING)
        val event =
            aTransactionStatusChangedEvent(
                externalTxId = transaction.externalTxId,
                previousStatus = "SUBMITTED",
                newStatus = "PROCESSING",
            )
        every { transactionRepository.findByExternalTxId(transaction.externalTxId) } returns transaction

        // when
        handler.handle(event)

        // then
        verify(exactly = 0) { transactionRepository.save(any()) }
    }

    @Test
    fun `should skip when transaction not found`() {
        // given
        val event = aTransactionStatusChangedEvent(externalTxId = "unknown-tx-id")
        every { transactionRepository.findByExternalTxId("unknown-tx-id") } returns null

        // when
        handler.handle(event)

        // then
        verify(exactly = 0) { transactionRepository.save(any()) }
    }

    @Test
    fun `should throw IllegalArgumentException for unknown status`() {
        // given
        val transaction = aTransaction(status = TransactionStatus.SUBMITTED)
        val event =
            aTransactionStatusChangedEvent(
                externalTxId = transaction.externalTxId,
                newStatus = "UNKNOWN_INVALID_STATUS",
            )
        every { transactionRepository.findByExternalTxId(transaction.externalTxId) } returns transaction

        // when/then
        assertThatThrownBy { handler.handle(event) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("Unknown transaction status: UNKNOWN_INVALID_STATUS")
    }

    @Test
    fun `should skip when transaction already in terminal status`() {
        // given
        val transaction = aTransaction(status = TransactionStatus.CONFIRMED)
        val event =
            aTransactionStatusChangedEvent(
                externalTxId = transaction.externalTxId,
                previousStatus = "CONFIRMING",
                newStatus = "FAILED",
            )
        every { transactionRepository.findByExternalTxId(transaction.externalTxId) } returns transaction

        // when
        handler.handle(event)

        // then
        verify(exactly = 0) { transactionRepository.save(any()) }
    }

    @Test
    fun `should update txHash when provided in event`() {
        // given
        val transaction = aTransaction(status = TransactionStatus.PROCESSING)
        val event =
            aTransactionStatusChangedEvent(
                externalTxId = transaction.externalTxId,
                previousStatus = "PROCESSING",
                newStatus = "CONFIRMING",
                fireblocksStatus = "CONFIRMING",
                txHash = "0xabc123",
            )
        every { transactionRepository.findByExternalTxId(transaction.externalTxId) } returns transaction
        every { transactionRepository.save(any()) } returnsArgument 0

        // when
        handler.handle(event)

        // then
        verify {
            transactionRepository.save(
                match {
                    it.status == TransactionStatus.CONFIRMING && it.txHash == "0xabc123"
                },
            )
        }
    }

    @Test
    fun `should use newStatus as fireblocksStatus when fireblocksStatus is null`() {
        // given
        val transaction = aTransaction(status = TransactionStatus.SUBMITTED)
        val event =
            aTransactionStatusChangedEvent(
                externalTxId = transaction.externalTxId,
                previousStatus = "SUBMITTED",
                newStatus = "PROCESSING",
                fireblocksStatus = null,
            )
        every { transactionRepository.findByExternalTxId(transaction.externalTxId) } returns transaction
        every { transactionRepository.save(any()) } returnsArgument 0

        // when
        handler.handle(event)

        // then
        verify {
            transactionRepository.save(
                match { it.fireblocksStatus == "PROCESSING" },
            )
        }
    }
}
