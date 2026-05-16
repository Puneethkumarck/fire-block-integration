package com.stablecoin.custody.fireblocks.infrastructure.scheduling

import com.stablecoin.custody.fireblocks.AbstractIntegrationTest
import com.stablecoin.custody.fireblocks.domain.transaction.TransactionRepository
import com.stablecoin.custody.fireblocks.domain.transaction.TransactionStatus
import com.stablecoin.custody.fireblocks.test.fixtures.aTransaction
import jakarta.persistence.EntityManager
import net.javacrumbs.shedlock.core.LockProvider
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.temporal.ChronoUnit

class TransactionPollingJobIntegrationTest : AbstractIntegrationTest() {
    @Autowired
    private lateinit var transactionPollingJob: TransactionPollingJob

    @Autowired
    private lateinit var transactionRepository: TransactionRepository

    @Autowired
    private lateinit var lockProvider: LockProvider

    @Autowired
    private lateinit var entityManager: EntityManager

    @Test
    fun `should wire scheduling infrastructure beans`() {
        // then
        assertThat(transactionPollingJob).isNotNull
        assertThat(lockProvider).isNotNull
    }

    @Test
    @Transactional
    fun `should not fail recent CREATED transactions`() {
        // given
        val recentTransaction =
            aTransaction(
                status = TransactionStatus.CREATED,
                createdAt = Instant.now(),
            )
        transactionRepository.save(recentTransaction)
        entityManager.flush()
        entityManager.clear()

        // when
        transactionPollingJob.recoverStaleCreatedTransactions()

        // then
        val result = transactionRepository.findById(recentTransaction.id)
        assertThat(result!!.status).isEqualTo(TransactionStatus.CREATED)
    }

    @Test
    @Transactional
    fun `should find stale created transactions for processing`() {
        // given
        val staleTransaction =
            aTransaction(
                status = TransactionStatus.CREATED,
                createdAt = Instant.now().minus(15, ChronoUnit.MINUTES),
            )
        transactionRepository.save(staleTransaction)
        entityManager.flush()
        entityManager.clear()

        // when
        val cutoff = Instant.now().minus(10, ChronoUnit.MINUTES)
        val staleCreated = transactionRepository.findStaleCreated(cutoff, 50)

        // then
        assertThat(staleCreated).hasSize(1)
        assertThat(staleCreated[0].id).isEqualTo(staleTransaction.id)
    }
}
