package com.stablecoin.custody.fireblocks.application.stream

import com.stablecoin.custody.fireblocks.domain.transaction.TransactionRepository
import com.stablecoin.custody.fireblocks.domain.transaction.TransactionStatus
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
class TransactionStatusEventConsumerTest {
    private val transactionRepository: TransactionRepository = mockk()
    private val consumer = TransactionStatusEventConsumer(transactionRepository)

    @Test
    fun `should process transaction status change event`() {
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
        consumer.consume(event)

        // then
        verify { transactionRepository.save(match { it.status == TransactionStatus.PROCESSING }) }
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
        consumer.consume(event)

        // then
        verify(exactly = 0) { transactionRepository.save(any()) }
    }

    @Test
    fun `should handle unknown transaction gracefully`() {
        // given
        val event = aTransactionStatusChangedEvent(externalTxId = "unknown-tx-id")
        every { transactionRepository.findByExternalTxId("unknown-tx-id") } returns null

        // when
        consumer.consume(event)

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

        // when / then
        assertThatThrownBy { consumer.consume(event) }
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
        consumer.consume(event)

        // then
        verify(exactly = 0) { transactionRepository.save(any()) }
    }
}
