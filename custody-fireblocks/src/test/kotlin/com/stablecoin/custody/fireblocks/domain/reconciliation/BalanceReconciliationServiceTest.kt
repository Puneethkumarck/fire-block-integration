package com.stablecoin.custody.fireblocks.domain.reconciliation

import com.stablecoin.custody.fireblocks.domain.audit.AuditLogRepository
import com.stablecoin.custody.fireblocks.domain.audit.AuditOperation
import com.stablecoin.custody.fireblocks.domain.audit.AuditStatus
import com.stablecoin.custody.fireblocks.domain.event.ReconciliationBreakDetectedEvent
import com.stablecoin.custody.fireblocks.domain.port.EventPublisher
import com.stablecoin.custody.fireblocks.test.fixtures.anInternalBalance
import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.math.BigDecimal

@ExtendWith(MockKExtension::class)
class BalanceReconciliationServiceTest {
    private val reconciliationResultRepository: ReconciliationResultRepository = mockk()
    private val internalBalanceRepository: InternalBalanceRepository = mockk()
    private val auditLogRepository: AuditLogRepository = mockk()
    private val breakEventPublisher: EventPublisher<ReconciliationBreakDetectedEvent> = mockk()

    private val service =
        BalanceReconciliationService(
            reconciliationResultRepository,
            internalBalanceRepository,
            auditLogRepository,
            breakEventPublisher,
        )

    @Nested
    inner class Reconcile {
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

    @Nested
    inner class CreatePartialResult {
        @Test
        fun `should return PARTIAL result with zero balances`() {
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
    }

    @Nested
    inner class PersistResult {
        @Test
        fun `should save result and audit log for MATCHED`() {
            // given
            val internalBalance = anInternalBalance(balance = BigDecimal("1000.00"))
            val result = service.reconcile(internalBalance, BigDecimal("1000.00"), BigDecimal("0.01"))

            every { reconciliationResultRepository.save(result) } returns result
            every { auditLogRepository.save(any()) } returnsArgument 0

            // when
            service.persistResult(result)

            // then
            verify { reconciliationResultRepository.save(result) }
            verify {
                auditLogRepository.save(
                    match {
                        it.operation == AuditOperation.RECONCILIATION_MATCHED &&
                            it.status == AuditStatus.SUCCESS
                    },
                )
            }
            verify(exactly = 0) { breakEventPublisher.publish(any()) }
        }

        @Test
        fun `should save result and publish break event for MISMATCHED`() {
            // given
            val internalBalance = anInternalBalance(balance = BigDecimal("1000.00"))
            val result = service.reconcile(internalBalance, BigDecimal("900.00"), BigDecimal("0.01"))

            every { reconciliationResultRepository.save(result) } returns result
            every { auditLogRepository.save(any()) } returnsArgument 0
            every { breakEventPublisher.publish(any()) } just runs

            // when
            service.persistResult(result)

            // then
            verify { reconciliationResultRepository.save(result) }
            verify {
                auditLogRepository.save(
                    match {
                        it.operation == AuditOperation.RECONCILIATION_MISMATCHED &&
                            it.status == AuditStatus.FAILURE
                    },
                )
            }
            verify {
                breakEventPublisher.publish(
                    match {
                        it.vaultId == internalBalance.vaultId &&
                            it.drift.compareTo(BigDecimal("-100.00")) == 0
                    },
                )
            }
        }

        @Test
        fun `should save result and audit log for PARTIAL`() {
            // given
            val vaultId = anInternalBalance().vaultId
            val result = service.createPartialResult(vaultId, "EURC", "ETH", BigDecimal("500.00"), BigDecimal("0.01"))

            every { reconciliationResultRepository.save(result) } returns result
            every { auditLogRepository.save(any()) } returnsArgument 0

            // when
            service.persistResult(result)

            // then
            verify { reconciliationResultRepository.save(result) }
            verify {
                auditLogRepository.save(
                    match {
                        it.operation == AuditOperation.RECONCILIATION_PARTIAL &&
                            it.status == AuditStatus.SUCCESS
                    },
                )
            }
            verify(exactly = 0) { breakEventPublisher.publish(any()) }
        }
    }

    @Nested
    inner class SeedAndPersist {
        @Test
        fun `should seed internal balance and persist matched result`() {
            // given
            val vaultId = anInternalBalance().vaultId
            val fireblocksAvailable = BigDecimal("500.00")
            val tolerance = BigDecimal("0.01")

            every { internalBalanceRepository.save(any()) } returnsArgument 0
            every { reconciliationResultRepository.save(any()) } returnsArgument 0
            every { auditLogRepository.save(any()) } returnsArgument 0

            // when
            service.seedAndPersist(vaultId, "EURC", "ETH", fireblocksAvailable, tolerance)

            // then
            verify {
                internalBalanceRepository.save(
                    match {
                        it.vaultId == vaultId &&
                            it.currency == "EURC" &&
                            it.protocol == "ETH" &&
                            it.balance == fireblocksAvailable
                    },
                )
            }
            verify {
                reconciliationResultRepository.save(
                    match { it.status == ReconciliationStatus.MATCHED },
                )
            }
            verify {
                auditLogRepository.save(
                    match { it.operation == AuditOperation.RECONCILIATION_MATCHED },
                )
            }
        }
    }
}
