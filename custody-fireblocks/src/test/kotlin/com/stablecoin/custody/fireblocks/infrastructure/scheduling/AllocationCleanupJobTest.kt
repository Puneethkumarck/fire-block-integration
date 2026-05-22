package com.stablecoin.custody.fireblocks.infrastructure.scheduling

import com.stablecoin.custody.fireblocks.domain.allocation.FundAllocationService
import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.Instant

@ExtendWith(MockKExtension::class)
class AllocationCleanupJobTest {
    private val fundAllocationService: FundAllocationService = mockk()

    private val job = AllocationCleanupJob(fundAllocationService)

    @Test
    fun `should delegate orphaned locked cleanup to fund allocation service`() {
        // given
        every { fundAllocationService.releaseOrphanedLocked(any<Instant>(), 50) } just runs
        every { fundAllocationService.failOrphanedPending(any<Instant>(), 50) } just runs

        // when
        job.cleanupOrphanedAllocations()

        // then
        verify { fundAllocationService.releaseOrphanedLocked(any<Instant>(), 50) }
    }

    @Test
    fun `should delegate orphaned pending cleanup to fund allocation service`() {
        // given
        every { fundAllocationService.releaseOrphanedLocked(any<Instant>(), 50) } just runs
        every { fundAllocationService.failOrphanedPending(any<Instant>(), 50) } just runs

        // when
        job.cleanupOrphanedAllocations()

        // then
        verify { fundAllocationService.failOrphanedPending(any<Instant>(), 50) }
    }
}
