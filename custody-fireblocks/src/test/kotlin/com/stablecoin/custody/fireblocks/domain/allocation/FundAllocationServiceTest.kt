package com.stablecoin.custody.fireblocks.domain.allocation

import com.stablecoin.custody.fireblocks.domain.audit.AuditLogRepository
import com.stablecoin.custody.fireblocks.domain.exception.AllocationAlreadyReleasedException
import com.stablecoin.custody.fireblocks.domain.exception.AllocationNotFoundException
import com.stablecoin.custody.fireblocks.domain.port.AllocationResult
import com.stablecoin.custody.fireblocks.domain.port.FireblocksVaultPort
import com.stablecoin.custody.fireblocks.test.fixtures.aFundAllocation
import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.math.BigDecimal
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

@ExtendWith(MockKExtension::class)
class FundAllocationServiceTest {
    private val fundAllocationRepository: FundAllocationRepository = mockk()
    private val fireblocksVaultPort: FireblocksVaultPort = mockk()
    private val auditLogRepository: AuditLogRepository = mockk()

    private val service =
        FundAllocationService(
            fundAllocationRepository,
            fireblocksVaultPort,
            auditLogRepository,
        )

    @Test
    fun `should create and lock allocation successfully`() {
        // given
        val allocationId = "alloc-001"
        val vaultId = UUID.randomUUID()
        val fireblocksVaultId = "fb-vault-123"
        val assetId = "BTC"
        val amount = BigDecimal("1.5")

        every { fundAllocationRepository.findByAllocationId(allocationId) } returns null
        every { fundAllocationRepository.save(any()) } returnsArgument 0
        every { fireblocksVaultPort.lockAllocation(any()) } returns AllocationResult(id = "lock-001", status = "LOCKED")
        every { auditLogRepository.save(any()) } returnsArgument 0

        // when
        val result =
            service.createAndLock(
                allocationId = allocationId,
                vaultId = vaultId,
                fireblocksVaultId = fireblocksVaultId,
                assetId = assetId,
                currency = "BTC",
                protocol = "BTC",
                amount = amount,
            )

        // then
        assertThat(result.status).isEqualTo(AllocationStatus.LOCKED)
        verify { fireblocksVaultPort.lockAllocation(any()) }
        verify(exactly = 2) { fundAllocationRepository.save(any()) }
    }

    @Test
    fun `should mark allocation as FAILED when lock fails`() {
        // given
        val allocationId = "alloc-002"
        val vaultId = UUID.randomUUID()

        every { fundAllocationRepository.findByAllocationId(allocationId) } returns null
        every { fundAllocationRepository.save(any()) } returnsArgument 0
        every { fireblocksVaultPort.lockAllocation(any()) } throws RuntimeException("Insufficient funds")
        every { auditLogRepository.save(any()) } returnsArgument 0

        // when
        val result =
            service.createAndLock(
                allocationId = allocationId,
                vaultId = vaultId,
                fireblocksVaultId = "fb-vault-123",
                assetId = "BTC",
                currency = "BTC",
                protocol = "BTC",
                amount = BigDecimal("1.5"),
            )

        // then
        assertThat(result.status).isEqualTo(AllocationStatus.FAILED)
        verify(exactly = 2) { fundAllocationRepository.save(any()) }
    }

    @Test
    fun `should return existing allocation when allocationId already exists`() {
        // given
        val existingAllocation = aFundAllocation(status = AllocationStatus.LOCKED)

        every { fundAllocationRepository.findByAllocationId(existingAllocation.allocationId) } returns existingAllocation

        // when
        val result =
            service.createAndLock(
                allocationId = existingAllocation.allocationId,
                vaultId = existingAllocation.vaultId,
                fireblocksVaultId = existingAllocation.fireblocksVaultId,
                assetId = existingAllocation.assetId,
                currency = existingAllocation.currency,
                protocol = existingAllocation.protocol,
                amount = existingAllocation.amount,
            )

        // then
        assertThat(result).usingRecursiveComparison().isEqualTo(existingAllocation)
        verify(exactly = 0) { fireblocksVaultPort.lockAllocation(any()) }
    }

    @Test
    fun `should consume allocation successfully`() {
        // given
        val allocation = aFundAllocation(status = AllocationStatus.LOCKED)
        val transactionId = UUID.randomUUID()

        every { fundAllocationRepository.findByAllocationId(allocation.allocationId) } returns allocation
        every { fundAllocationRepository.save(any()) } returnsArgument 0
        every { auditLogRepository.save(any()) } returnsArgument 0

        // when
        val result = service.consume(allocation.allocationId, transactionId)

        // then
        val expected = allocation.copy(status = AllocationStatus.CONSUMED, transactionId = transactionId)
        assertThat(result)
            .usingRecursiveComparison()
            .ignoringFields("updatedAt")
            .isEqualTo(expected)
    }

    @Test
    fun `should throw AllocationNotFoundException when consuming unknown allocation`() {
        // given
        val allocationId = "unknown-alloc"
        every { fundAllocationRepository.findByAllocationId(allocationId) } returns null

        // when/then
        assertThatThrownBy { service.consume(allocationId, UUID.randomUUID()) }
            .isInstanceOf(AllocationNotFoundException::class.java)
            .hasMessageContaining(allocationId)
    }

    @Test
    fun `should release allocation successfully`() {
        // given
        val allocation = aFundAllocation(status = AllocationStatus.LOCKED)

        every { fundAllocationRepository.findByAllocationId(allocation.allocationId) } returns allocation
        every { fireblocksVaultPort.releaseAllocation(any()) } just runs
        every { fundAllocationRepository.save(any()) } returnsArgument 0
        every { auditLogRepository.save(any()) } returnsArgument 0

        // when
        val result = service.release(allocation.allocationId)

        // then
        val expected = allocation.copy(status = AllocationStatus.RELEASED)
        assertThat(result)
            .usingRecursiveComparison()
            .ignoringFields("updatedAt")
            .isEqualTo(expected)
        verify { fireblocksVaultPort.releaseAllocation(any()) }
    }

    @Test
    fun `should throw AllocationAlreadyReleasedException when releasing already released allocation`() {
        // given
        val allocation = aFundAllocation(status = AllocationStatus.RELEASED)

        every { fundAllocationRepository.findByAllocationId(allocation.allocationId) } returns allocation

        // when/then
        assertThatThrownBy { service.release(allocation.allocationId) }
            .isInstanceOf(AllocationAlreadyReleasedException::class.java)
            .hasMessageContaining(allocation.allocationId)
    }

    @Test
    fun `should throw AllocationNotFoundException when releasing unknown allocation`() {
        // given
        val allocationId = "unknown-alloc"
        every { fundAllocationRepository.findByAllocationId(allocationId) } returns null

        // when/then
        assertThatThrownBy { service.release(allocationId) }
            .isInstanceOf(AllocationNotFoundException::class.java)
            .hasMessageContaining(allocationId)
    }

    @Test
    fun `should release allocation by transactionId successfully`() {
        // given
        val transactionId = UUID.randomUUID()
        val allocation = aFundAllocation(status = AllocationStatus.CONSUMED, transactionId = transactionId)

        every { fundAllocationRepository.findByTransactionId(transactionId) } returns allocation
        every { fireblocksVaultPort.releaseAllocation(any()) } just runs
        every { fundAllocationRepository.save(any()) } returnsArgument 0
        every { auditLogRepository.save(any()) } returnsArgument 0

        // when
        service.releaseByTransactionId(transactionId)

        // then
        verify { fireblocksVaultPort.releaseAllocation(any()) }
        verify { fundAllocationRepository.save(match { it.status == AllocationStatus.RELEASED }) }
    }

    @Test
    fun `should be noop when releaseByTransactionId finds no allocation`() {
        // given
        val transactionId = UUID.randomUUID()
        every { fundAllocationRepository.findByTransactionId(transactionId) } returns null

        // when
        service.releaseByTransactionId(transactionId)

        // then
        verify(exactly = 0) { fireblocksVaultPort.releaseAllocation(any()) }
        verify(exactly = 0) { fundAllocationRepository.save(any()) }
    }

    @Test
    fun `should be noop when releaseByTransactionId finds terminal allocation`() {
        // given
        val transactionId = UUID.randomUUID()
        val allocation = aFundAllocation(status = AllocationStatus.RELEASED, transactionId = transactionId)

        every { fundAllocationRepository.findByTransactionId(transactionId) } returns allocation

        // when
        service.releaseByTransactionId(transactionId)

        // then
        verify(exactly = 0) { fireblocksVaultPort.releaseAllocation(any()) }
        verify(exactly = 0) { fundAllocationRepository.save(any()) }
    }

    @Test
    fun `should release orphaned locked allocations`() {
        // given
        val cutoff = Instant.now().minus(5, ChronoUnit.MINUTES)
        val allocation1 = aFundAllocation(status = AllocationStatus.LOCKED)
        val allocation2 = aFundAllocation(status = AllocationStatus.LOCKED)

        every { fundAllocationRepository.findOrphanedLocked(cutoff, 50) } returns listOf(allocation1, allocation2)
        every { fireblocksVaultPort.releaseAllocation(any()) } just runs
        every { fundAllocationRepository.save(any()) } returnsArgument 0
        every { auditLogRepository.save(any()) } returnsArgument 0

        // when
        service.releaseOrphanedLocked(cutoff, 50)

        // then
        verify(exactly = 2) { fireblocksVaultPort.releaseAllocation(any()) }
        verify(exactly = 2) { fundAllocationRepository.save(match { it.status == AllocationStatus.RELEASED }) }
    }

    @Test
    fun `should continue processing remaining allocations when one release fails`() {
        // given
        val cutoff = Instant.now().minus(5, ChronoUnit.MINUTES)
        val allocation1 = aFundAllocation(status = AllocationStatus.LOCKED)
        val allocation2 = aFundAllocation(status = AllocationStatus.LOCKED)

        every { fundAllocationRepository.findOrphanedLocked(cutoff, 50) } returns listOf(allocation1, allocation2)
        every { fireblocksVaultPort.releaseAllocation(match { it.allocationId == allocation1.allocationId }) } throws
            RuntimeException("Release failed")
        every { fireblocksVaultPort.releaseAllocation(match { it.allocationId == allocation2.allocationId }) } just runs
        every { fundAllocationRepository.save(any()) } returnsArgument 0
        every { auditLogRepository.save(any()) } returnsArgument 0

        // when
        service.releaseOrphanedLocked(cutoff, 50)

        // then
        verify(exactly = 1) { fundAllocationRepository.save(match { it.status == AllocationStatus.RELEASED }) }
    }

    @Test
    fun `should fail orphaned pending allocations`() {
        // given
        val cutoff = Instant.now().minus(5, ChronoUnit.MINUTES)
        val allocation1 = aFundAllocation(status = AllocationStatus.PENDING)
        val allocation2 = aFundAllocation(status = AllocationStatus.PENDING)

        every { fundAllocationRepository.findOrphanedPending(cutoff, 50) } returns listOf(allocation1, allocation2)
        every { fundAllocationRepository.save(any()) } returnsArgument 0
        every { auditLogRepository.save(any()) } returnsArgument 0

        // when
        service.failOrphanedPending(cutoff, 50)

        // then
        verify(exactly = 2) { fundAllocationRepository.save(match { it.status == AllocationStatus.FAILED }) }
    }

    @Test
    fun `should continue processing remaining allocations when one fail operation errors`() {
        // given
        val cutoff = Instant.now().minus(5, ChronoUnit.MINUTES)
        val allocation1 = aFundAllocation(status = AllocationStatus.PENDING)
        val allocation2 = aFundAllocation(status = AllocationStatus.PENDING)

        every { fundAllocationRepository.findOrphanedPending(cutoff, 50) } returns listOf(allocation1, allocation2)
        every { fundAllocationRepository.save(match { it.allocationId == allocation1.allocationId }) } throws
            RuntimeException("Save failed")
        every { fundAllocationRepository.save(match { it.allocationId == allocation2.allocationId }) } returnsArgument 0
        every { auditLogRepository.save(any()) } returnsArgument 0

        // when
        service.failOrphanedPending(cutoff, 50)

        // then
        verify(exactly = 1) { auditLogRepository.save(any()) }
    }
}
