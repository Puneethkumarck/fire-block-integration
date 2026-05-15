package com.stablecoin.custody.fireblocks.domain.transaction

import com.stablecoin.custody.fireblocks.test.fixtures.aSubmitTransactionCommand
import com.stablecoin.custody.fireblocks.test.fixtures.aTransaction
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class TransactionTest {
    @Test
    fun `should create transaction in CREATED status`() {
        // given
        val command = aSubmitTransactionCommand()

        // when
        val result = Transaction.create(command, "BTC")

        // then
        val expected =
            aTransaction(
                externalTxId = command.externalTxId,
                fireblocksTransactionId = null,
                status = TransactionStatus.CREATED,
                fireblocksStatus = null,
                fireblocksSubStatus = null,
                assetId = "BTC",
                currency = command.currency,
                protocol = command.protocol,
                amount = command.amount,
                sourceVaultId = command.sourceVaultId,
                destinationAddress = command.destinationAddress,
                feeLevel = FeeLevel.MEDIUM,
                treatAsGrossAmount = false,
                txHash = null,
                note = null,
            )
        assertThat(result)
            .usingRecursiveComparison()
            .ignoringFields("id", "createdAt", "updatedAt")
            .isEqualTo(expected)
    }

    @Test
    fun `should mark transaction as submitted`() {
        // given
        val transaction = aTransaction(status = TransactionStatus.CREATED)

        // when
        val result = transaction.markSubmitted("fb-tx-123")

        // then
        val expected =
            transaction.copy(
                fireblocksTransactionId = "fb-tx-123",
                status = TransactionStatus.SUBMITTED,
            )
        assertThat(result)
            .usingRecursiveComparison()
            .ignoringFields("updatedAt")
            .isEqualTo(expected)
    }

    @Test
    fun `should mark transaction as failed from CREATED`() {
        // given
        val transaction = aTransaction(status = TransactionStatus.CREATED)

        // when
        val result = transaction.markFailed()

        // then
        val expected = transaction.copy(status = TransactionStatus.FAILED)
        assertThat(result)
            .usingRecursiveComparison()
            .ignoringFields("updatedAt")
            .isEqualTo(expected)
    }

    @Test
    fun `should update status with Fireblocks data`() {
        // given
        val transaction = aTransaction(status = TransactionStatus.SUBMITTED, fireblocksTransactionId = "fb-tx-123")

        // when
        val result =
            transaction.updateStatus(
                newStatus = TransactionStatus.PROCESSING,
                fireblocksStatus = "PENDING_SIGNATURE",
                fireblocksSubStatus = "PENDING_APPROVAL",
                txHash = "0xabc123",
            )

        // then
        val expected =
            transaction.copy(
                status = TransactionStatus.PROCESSING,
                fireblocksStatus = "PENDING_SIGNATURE",
                fireblocksSubStatus = "PENDING_APPROVAL",
                txHash = "0xabc123",
            )
        assertThat(result)
            .usingRecursiveComparison()
            .ignoringFields("updatedAt")
            .isEqualTo(expected)
    }

    @Test
    fun `should reject blank externalTxId`() {
        // when/then
        assertThatThrownBy { aTransaction(externalTxId = " ") }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("externalTxId must not be blank")
    }

    @Test
    fun `should reject zero amount`() {
        // when/then
        assertThatThrownBy { aTransaction(amount = BigDecimal.ZERO) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("amount must be positive")
    }

    @Test
    fun `should reject negative amount`() {
        // when/then
        assertThatThrownBy { aTransaction(amount = BigDecimal("-1.0")) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("amount must be positive")
    }

    @Test
    fun `should reject blank destinationAddress`() {
        // when/then
        assertThatThrownBy { aTransaction(destinationAddress = "") }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("destinationAddress must not be blank")
    }

    @Test
    fun `should default feeLevel to MEDIUM`() {
        // given
        val command = aSubmitTransactionCommand(feeLevel = null)

        // when
        val result = Transaction.create(command, "BTC")

        // then
        assertThat(result.feeLevel).isEqualTo(FeeLevel.MEDIUM)
    }

    @Test
    fun `should default treatAsGrossAmount to false`() {
        // given
        val command = aSubmitTransactionCommand(treatAsGrossAmount = null)

        // when
        val result = Transaction.create(command, "BTC")

        // then
        assertThat(result.treatAsGrossAmount).isFalse()
    }
}
