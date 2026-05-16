package com.stablecoin.custody.fireblocks.domain.transaction

import com.stablecoin.custody.fireblocks.domain.exception.TransactionNotFoundException
import com.stablecoin.custody.fireblocks.test.fixtures.aTransaction
import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class TransactionQueryServiceTest {
    private val transactionRepository: TransactionRepository = mockk()

    private val service = TransactionQueryService(transactionRepository)

    @Test
    fun `should find transaction by externalTxId`() {
        // given
        val transaction = aTransaction(externalTxId = "ext-tx-001")
        every { transactionRepository.findByExternalTxId("ext-tx-001") } returns transaction

        // when
        val result = service.getByExternalTxId("ext-tx-001")

        // then
        assertThat(result).usingRecursiveComparison().isEqualTo(transaction)
    }

    @Test
    fun `should find transaction by fireblocksTxId`() {
        // given
        val transaction = aTransaction(fireblocksTransactionId = "fb-tx-001")
        every { transactionRepository.findByFireblocksTransactionId("fb-tx-001") } returns transaction

        // when
        val result = service.getByFireblocksTxId("fb-tx-001")

        // then
        assertThat(result).usingRecursiveComparison().isEqualTo(transaction)
    }

    @Test
    fun `should throw TransactionNotFoundException when not found`() {
        // given
        every { transactionRepository.findByExternalTxId("unknown-tx") } returns null

        // when/then
        assertThatThrownBy { service.getByExternalTxId("unknown-tx") }
            .isInstanceOf(TransactionNotFoundException::class.java)
            .hasMessageContaining("unknown-tx")
    }
}
