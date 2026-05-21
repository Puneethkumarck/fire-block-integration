package com.stablecoin.custody.fireblocks.domain.allocation

import com.stablecoin.custody.fireblocks.test.fixtures.aFundAllocation
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.util.UUID

class FundAllocationTest {
    @Test
    fun `should create allocation with PENDING status`() {
        // when
        val result =
            FundAllocation.create(
                allocationId = "alloc-001",
                vaultId = UUID.randomUUID(),
                fireblocksVaultId = "fb-vault-123",
                assetId = "BTC",
                currency = "BTC",
                protocol = "BTC",
                amount = BigDecimal("1.5"),
            )

        // then
        val expected =
            aFundAllocation(
                allocationId = "alloc-001",
                fireblocksVaultId = "fb-vault-123",
                assetId = "BTC",
                currency = "BTC",
                protocol = "BTC",
                amount = BigDecimal("1.5"),
                status = AllocationStatus.PENDING,
                transactionId = null,
            )
        assertThat(result)
            .usingRecursiveComparison()
            .ignoringFields("id", "vaultId", "createdAt", "updatedAt")
            .isEqualTo(expected)
    }

    @Test
    fun `should transition from PENDING to LOCKED`() {
        // given
        val allocation = aFundAllocation(status = AllocationStatus.PENDING)

        // when
        val result = allocation.lock()

        // then
        val expected = allocation.copy(status = AllocationStatus.LOCKED)
        assertThat(result)
            .usingRecursiveComparison()
            .ignoringFields("updatedAt")
            .isEqualTo(expected)
    }

    @Test
    fun `should transition from LOCKED to CONSUMED with transactionId`() {
        // given
        val allocation = aFundAllocation(status = AllocationStatus.LOCKED)
        val transactionId = UUID.randomUUID()

        // when
        val result = allocation.consume(transactionId)

        // then
        val expected = allocation.copy(status = AllocationStatus.CONSUMED, transactionId = transactionId)
        assertThat(result)
            .usingRecursiveComparison()
            .ignoringFields("updatedAt")
            .isEqualTo(expected)
    }

    @Test
    fun `should transition from LOCKED to RELEASED`() {
        // given
        val allocation = aFundAllocation(status = AllocationStatus.LOCKED)

        // when
        val result = allocation.release()

        // then
        val expected = allocation.copy(status = AllocationStatus.RELEASED)
        assertThat(result)
            .usingRecursiveComparison()
            .ignoringFields("updatedAt")
            .isEqualTo(expected)
    }

    @Test
    fun `should transition from CONSUMED to RELEASED`() {
        // given
        val transactionId = UUID.randomUUID()
        val allocation = aFundAllocation(status = AllocationStatus.CONSUMED, transactionId = transactionId)

        // when
        val result = allocation.release()

        // then
        val expected = allocation.copy(status = AllocationStatus.RELEASED)
        assertThat(result)
            .usingRecursiveComparison()
            .ignoringFields("updatedAt")
            .isEqualTo(expected)
    }

    @Test
    fun `should transition from PENDING to FAILED`() {
        // given
        val allocation = aFundAllocation(status = AllocationStatus.PENDING)

        // when
        val result = allocation.markFailed()

        // then
        val expected = allocation.copy(status = AllocationStatus.FAILED)
        assertThat(result)
            .usingRecursiveComparison()
            .ignoringFields("updatedAt")
            .isEqualTo(expected)
    }

    @Test
    fun `should throw on invalid transition from PENDING to CONSUMED`() {
        // given
        val allocation = aFundAllocation(status = AllocationStatus.PENDING)

        // when/then
        assertThatThrownBy { allocation.consume(UUID.randomUUID()) }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessageContaining("Cannot transition allocation")
    }

    @Test
    fun `should throw on invalid transition from FAILED to LOCKED`() {
        // given
        val allocation = aFundAllocation(status = AllocationStatus.FAILED)

        // when/then
        assertThatThrownBy { allocation.lock() }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessageContaining("Cannot transition allocation")
    }

    @Test
    fun `should throw on invalid transition from RELEASED to CONSUMED`() {
        // given
        val allocation = aFundAllocation(status = AllocationStatus.RELEASED)

        // when/then
        assertThatThrownBy { allocation.consume(UUID.randomUUID()) }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessageContaining("Cannot transition allocation")
    }

    @Test
    fun `should validate allocationId not blank`() {
        // when/then
        assertThatThrownBy { aFundAllocation(allocationId = "") }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("allocationId must not be blank")
    }

    @Test
    fun `should validate amount positive`() {
        // when/then
        assertThatThrownBy { aFundAllocation(amount = BigDecimal.ZERO) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("amount must be positive")
    }

    @Test
    fun `should reject negative amount`() {
        // when/then
        assertThatThrownBy { aFundAllocation(amount = BigDecimal("-1")) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("amount must be positive")
    }
}
