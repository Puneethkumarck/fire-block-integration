package com.stablecoin.custody.fireblocks.domain.reconciliation

import com.stablecoin.custody.fireblocks.test.fixtures.anInternalBalance
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class BalanceReconciliationServiceTest {
    private val service = BalanceReconciliationService()

    @Test
    fun `should return MATCHED when drift within tolerance`() {
        // given
        val internalBalance = anInternalBalance(balance = BigDecimal("1000.00"))
        val fireblocksAvailable = BigDecimal("1000.005")
        val tolerance = BigDecimal("0.01")

        // when
        val result = service.reconcile(internalBalance, fireblocksAvailable, tolerance)

        // then
        val expected =
            ReconciliationResult(
                id = result.id,
                vaultId = internalBalance.vaultId,
                currency = internalBalance.currency,
                protocol = internalBalance.protocol,
                internalBalance = BigDecimal("1000.00"),
                fireblocksBalance = BigDecimal("1000.005"),
                drift = BigDecimal("0.005"),
                absoluteDrift = BigDecimal("0.005"),
                status = ReconciliationStatus.MATCHED,
                toleranceUsed = BigDecimal("0.01"),
                createdAt = result.createdAt,
            )
        assertThat(result).usingRecursiveComparison().isEqualTo(expected)
    }

    @Test
    fun `should return MISMATCHED when drift exceeds tolerance`() {
        // given
        val internalBalance = anInternalBalance(balance = BigDecimal("1000.00"))
        val fireblocksAvailable = BigDecimal("1000.05")
        val tolerance = BigDecimal("0.01")

        // when
        val result = service.reconcile(internalBalance, fireblocksAvailable, tolerance)

        // then
        val expected =
            ReconciliationResult(
                id = result.id,
                vaultId = internalBalance.vaultId,
                currency = internalBalance.currency,
                protocol = internalBalance.protocol,
                internalBalance = BigDecimal("1000.00"),
                fireblocksBalance = BigDecimal("1000.05"),
                drift = BigDecimal("0.05"),
                absoluteDrift = BigDecimal("0.05"),
                status = ReconciliationStatus.MISMATCHED,
                toleranceUsed = BigDecimal("0.01"),
                createdAt = result.createdAt,
            )
        assertThat(result).usingRecursiveComparison().isEqualTo(expected)
    }

    @Test
    fun `should return PARTIAL result when fireblocks unavailable`() {
        // given
        val vaultId = anInternalBalance().vaultId
        val internalBal = BigDecimal("500.00")
        val tolerance = BigDecimal("0.01")

        // when
        val result = service.createPartialResult(vaultId, "EURC", "ETH", internalBal, tolerance)

        // then
        val expected =
            ReconciliationResult(
                id = result.id,
                vaultId = vaultId,
                currency = "EURC",
                protocol = "ETH",
                internalBalance = BigDecimal("500.00"),
                fireblocksBalance = BigDecimal.ZERO,
                drift = BigDecimal.ZERO,
                absoluteDrift = BigDecimal.ZERO,
                status = ReconciliationStatus.PARTIAL,
                toleranceUsed = BigDecimal("0.01"),
                createdAt = result.createdAt,
            )
        assertThat(result).usingRecursiveComparison().isEqualTo(expected)
    }

    @Test
    fun `should compute positive drift when fireblocks has more`() {
        // given
        val internalBalance = anInternalBalance(balance = BigDecimal("100.00"))
        val fireblocksAvailable = BigDecimal("150.00")
        val tolerance = BigDecimal("0.01")

        // when
        val result = service.reconcile(internalBalance, fireblocksAvailable, tolerance)

        // then
        val expected =
            ReconciliationResult(
                id = result.id,
                vaultId = internalBalance.vaultId,
                currency = internalBalance.currency,
                protocol = internalBalance.protocol,
                internalBalance = BigDecimal("100.00"),
                fireblocksBalance = BigDecimal("150.00"),
                drift = BigDecimal("50.00"),
                absoluteDrift = BigDecimal("50.00"),
                status = ReconciliationStatus.MISMATCHED,
                toleranceUsed = BigDecimal("0.01"),
                createdAt = result.createdAt,
            )
        assertThat(result).usingRecursiveComparison().isEqualTo(expected)
    }

    @Test
    fun `should compute negative drift when internal has more`() {
        // given
        val internalBalance = anInternalBalance(balance = BigDecimal("200.00"))
        val fireblocksAvailable = BigDecimal("100.00")
        val tolerance = BigDecimal("0.01")

        // when
        val result = service.reconcile(internalBalance, fireblocksAvailable, tolerance)

        // then
        val expected =
            ReconciliationResult(
                id = result.id,
                vaultId = internalBalance.vaultId,
                currency = internalBalance.currency,
                protocol = internalBalance.protocol,
                internalBalance = BigDecimal("200.00"),
                fireblocksBalance = BigDecimal("100.00"),
                drift = BigDecimal("-100.00"),
                absoluteDrift = BigDecimal("100.00"),
                status = ReconciliationStatus.MISMATCHED,
                toleranceUsed = BigDecimal("0.01"),
                createdAt = result.createdAt,
            )
        assertThat(result).usingRecursiveComparison().isEqualTo(expected)
    }

    @Test
    fun `should return MATCHED when drift exactly equals tolerance`() {
        // given
        val internalBalance = anInternalBalance(balance = BigDecimal("1000.00"))
        val fireblocksAvailable = BigDecimal("1000.01")
        val tolerance = BigDecimal("0.01")

        // when
        val result = service.reconcile(internalBalance, fireblocksAvailable, tolerance)

        // then
        val expected =
            ReconciliationResult(
                id = result.id,
                vaultId = internalBalance.vaultId,
                currency = internalBalance.currency,
                protocol = internalBalance.protocol,
                internalBalance = BigDecimal("1000.00"),
                fireblocksBalance = BigDecimal("1000.01"),
                drift = BigDecimal("0.01"),
                absoluteDrift = BigDecimal("0.01"),
                status = ReconciliationStatus.MATCHED,
                toleranceUsed = BigDecimal("0.01"),
                createdAt = result.createdAt,
            )
        assertThat(result).usingRecursiveComparison().isEqualTo(expected)
    }
}
