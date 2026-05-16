package com.stablecoin.custody.fireblocks.infrastructure.scheduling

import com.stablecoin.custody.fireblocks.domain.port.FireblocksTransactionPort
import com.stablecoin.custody.fireblocks.domain.port.TransactionResult
import com.stablecoin.custody.fireblocks.domain.transaction.TransactionRepository
import com.stablecoin.custody.fireblocks.domain.transaction.TransactionStatus
import com.stablecoin.custody.fireblocks.domain.transaction.TransactionStatusHandler
import com.stablecoin.custody.fireblocks.test.fixtures.aTransaction
import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class TransactionPollingJobTest {
    private val transactionRepository: TransactionRepository = mockk()
    private val fireblocksTransactionPort: FireblocksTransactionPort = mockk()
    private val transactionStatusHandler: TransactionStatusHandler = mockk()

    private val job =
        TransactionPollingJob(
            transactionRepository,
            fireblocksTransactionPort,
            transactionStatusHandler,
        )

    @Test
    fun `should poll stale transactions and update status`() {
        // given
        val transaction =
            aTransaction(
                status = TransactionStatus.SUBMITTED,
                fireblocksTransactionId = "fb-tx-001",
            )
        every { transactionRepository.findStaleNonTerminal(any(), 50) } returns listOf(transaction)
        every { fireblocksTransactionPort.getTransaction("fb-tx-001") } returns
            TransactionResult(id = "fb-tx-001", status = "CONFIRMING", subStatus = null, txHash = "0xhash")
        every { transactionStatusHandler.handleStatusUpdate(any(), any(), any(), any()) } just runs

        // when
        job.pollStaleTransactions()

        // then
        verify {
            transactionStatusHandler.handleStatusUpdate(
                fireblocksTxId = "fb-tx-001",
                fireblocksStatus = "CONFIRMING",
                subStatus = null,
                txHash = "0xhash",
            )
        }
    }

    @Test
    fun `should skip transactions without fireblocksTransactionId`() {
        // given
        val transaction =
            aTransaction(
                status = TransactionStatus.CREATED,
                fireblocksTransactionId = null,
            )
        every { transactionRepository.findStaleNonTerminal(any(), 50) } returns listOf(transaction)

        // when
        job.pollStaleTransactions()

        // then
        verify(exactly = 0) { fireblocksTransactionPort.getTransaction(any()) }
    }

    @Test
    fun `should not poll when no stale transactions exist`() {
        // given
        every { transactionRepository.findStaleNonTerminal(any(), 50) } returns emptyList()

        // when
        job.pollStaleTransactions()

        // then
        verify(exactly = 0) { fireblocksTransactionPort.getTransaction(any()) }
    }

    @Test
    fun `should continue processing when single transaction poll fails`() {
        // given
        val transaction1 =
            aTransaction(
                status = TransactionStatus.SUBMITTED,
                fireblocksTransactionId = "fb-tx-001",
            )
        val transaction2 =
            aTransaction(
                status = TransactionStatus.SUBMITTED,
                fireblocksTransactionId = "fb-tx-002",
            )
        every { transactionRepository.findStaleNonTerminal(any(), 50) } returns
            listOf(transaction1, transaction2)
        every { fireblocksTransactionPort.getTransaction("fb-tx-001") } throws
            RuntimeException("API error")
        every { fireblocksTransactionPort.getTransaction("fb-tx-002") } returns
            TransactionResult(id = "fb-tx-002", status = "COMPLETED", subStatus = null, txHash = "0xhash2")
        every { transactionStatusHandler.handleStatusUpdate(any(), any(), any(), any()) } just runs

        // when
        job.pollStaleTransactions()

        // then
        verify {
            transactionStatusHandler.handleStatusUpdate(
                fireblocksTxId = "fb-tx-002",
                fireblocksStatus = "COMPLETED",
                subStatus = null,
                txHash = "0xhash2",
            )
        }
    }

    @Test
    fun `should fail stale created transactions`() {
        // given
        val transaction = aTransaction(status = TransactionStatus.CREATED)
        every { transactionRepository.findStaleCreated(any(), 50) } returns listOf(transaction)
        every { transactionRepository.save(any()) } returnsArgument 0

        // when
        job.failStaleCreatedTransactions()

        // then
        verify {
            transactionRepository.save(match { it.status == TransactionStatus.FAILED })
        }
    }

    @Test
    fun `should not fail when no stale created transactions exist`() {
        // given
        every { transactionRepository.findStaleCreated(any(), 50) } returns emptyList()

        // when
        job.failStaleCreatedTransactions()

        // then
        verify(exactly = 0) { transactionRepository.save(any()) }
    }
}
