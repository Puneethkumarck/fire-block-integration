package com.stablecoin.custody.fireblocks.infrastructure.persistence

import com.stablecoin.custody.fireblocks.AbstractIntegrationTest
import com.stablecoin.custody.fireblocks.domain.transaction.TransactionId
import com.stablecoin.custody.fireblocks.domain.transaction.TransactionRepository
import com.stablecoin.custody.fireblocks.domain.transaction.TransactionStatus
import com.stablecoin.custody.fireblocks.test.fixtures.aTransaction
import jakarta.persistence.EntityManager
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

@Transactional
class TransactionPersistenceIntegrationTest : AbstractIntegrationTest() {
    @Autowired
    private lateinit var transactionRepository: TransactionRepository

    @Autowired
    private lateinit var entityManager: EntityManager

    @Test
    fun `should save and retrieve transaction by ID`() {
        // given
        val transaction = aTransaction()
        val saved = transactionRepository.save(transaction)
        entityManager.clear()

        // when
        val result = transactionRepository.findById(saved.id)

        // then
        assertThat(result)
            .usingRecursiveComparison()
            .withComparatorForType(Comparator.naturalOrder(), BigDecimal::class.java)
            .ignoringFields("createdAt", "updatedAt")
            .isEqualTo(saved)
    }

    @Test
    fun `should find transaction by externalTxId`() {
        // given
        val externalTxId = "ext-tx-${UUID.randomUUID()}"
        val transaction = aTransaction(externalTxId = externalTxId)
        val saved = transactionRepository.save(transaction)
        entityManager.clear()

        // when
        val result = transactionRepository.findByExternalTxId(externalTxId)

        // then
        assertThat(result)
            .usingRecursiveComparison()
            .withComparatorForType(Comparator.naturalOrder(), BigDecimal::class.java)
            .ignoringFields("createdAt", "updatedAt")
            .isEqualTo(saved)
    }

    @Test
    fun `should find transaction by fireblocksTxId`() {
        // given
        val transaction = aTransaction(fireblocksTransactionId = "fb-tx-${UUID.randomUUID()}")
        val saved = transactionRepository.save(transaction)
        entityManager.clear()

        // when
        val result = transactionRepository.findByFireblocksTransactionId(saved.fireblocksTransactionId!!)

        // then
        assertThat(result)
            .usingRecursiveComparison()
            .withComparatorForType(Comparator.naturalOrder(), BigDecimal::class.java)
            .ignoringFields("createdAt", "updatedAt")
            .isEqualTo(saved)
    }

    @Test
    fun `should enforce unique externalTxId constraint`() {
        // given
        val externalTxId = "duplicate-tx-${UUID.randomUUID()}"
        transactionRepository.save(aTransaction(externalTxId = externalTxId))

        // when/then
        assertThatThrownBy {
            transactionRepository.save(aTransaction(externalTxId = externalTxId))
        }.isInstanceOf(DataIntegrityViolationException::class.java)
    }

    @Test
    fun `should acquire pessimistic lock with findByIdForUpdate`() {
        // given
        val transaction = aTransaction()
        val saved = transactionRepository.save(transaction)
        entityManager.clear()

        // when
        val result = transactionRepository.findByIdForUpdate(saved.id)

        // then
        assertThat(result)
            .usingRecursiveComparison()
            .withComparatorForType(Comparator.naturalOrder(), BigDecimal::class.java)
            .ignoringFields("createdAt", "updatedAt")
            .isEqualTo(saved)
    }

    @Test
    fun `should find stale non-terminal transactions`() {
        // given
        val staleProcessing =
            aTransaction(
                status = TransactionStatus.PROCESSING,
                updatedAt = Instant.now().minus(2, ChronoUnit.HOURS),
            )
        val recentProcessing =
            aTransaction(
                status = TransactionStatus.PROCESSING,
                updatedAt = Instant.now(),
            )
        val staleConfirmed =
            aTransaction(
                status = TransactionStatus.CONFIRMED,
                updatedAt = Instant.now().minus(2, ChronoUnit.HOURS),
            )
        val savedStale = transactionRepository.save(staleProcessing)
        transactionRepository.save(recentProcessing)
        transactionRepository.save(staleConfirmed)
        entityManager.clear()

        // when
        val cutoff = Instant.now().minus(1, ChronoUnit.HOURS)
        val result = transactionRepository.findStaleNonTerminal(cutoff, 10)

        // then
        assertThat(result).hasSize(1)
        assertThat(result[0])
            .usingRecursiveComparison()
            .withComparatorForType(Comparator.naturalOrder(), BigDecimal::class.java)
            .ignoringFields("createdAt", "updatedAt")
            .isEqualTo(savedStale)
    }

    @Test
    fun `should find stale CREATED transactions`() {
        // given
        val staleCreated =
            aTransaction(
                status = TransactionStatus.CREATED,
                createdAt = Instant.now().minus(2, ChronoUnit.HOURS),
            )
        val recentCreated =
            aTransaction(
                status = TransactionStatus.CREATED,
                createdAt = Instant.now(),
            )
        val staleSubmitted =
            aTransaction(
                status = TransactionStatus.SUBMITTED,
                createdAt = Instant.now().minus(2, ChronoUnit.HOURS),
            )
        val savedStale = transactionRepository.save(staleCreated)
        transactionRepository.save(recentCreated)
        transactionRepository.save(staleSubmitted)
        entityManager.clear()

        // when
        val cutoff = Instant.now().minus(1, ChronoUnit.HOURS)
        val result = transactionRepository.findStaleCreated(cutoff, 10)

        // then
        assertThat(result).hasSize(1)
        assertThat(result[0])
            .usingRecursiveComparison()
            .withComparatorForType(Comparator.naturalOrder(), BigDecimal::class.java)
            .ignoringFields("createdAt", "updatedAt")
            .isEqualTo(savedStale)
    }

    @Test
    fun `should persist BigDecimal amount with full precision`() {
        // given
        val preciseAmount = BigDecimal("123456789012345678.123456789012345678")
        val transaction = aTransaction(amount = preciseAmount)
        transactionRepository.save(transaction)
        entityManager.clear()

        // when
        val result = transactionRepository.findById(transaction.id)

        // then
        assertThat(result!!.amount).isEqualByComparingTo(preciseAmount)
    }

    @Test
    fun `should return null when transaction not found`() {
        // when
        val result = transactionRepository.findById(TransactionId(UUID.randomUUID()))

        // then
        assertThat(result).isNull()
    }
}
