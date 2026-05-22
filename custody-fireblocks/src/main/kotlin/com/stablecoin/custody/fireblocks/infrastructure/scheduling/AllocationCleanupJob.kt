package com.stablecoin.custody.fireblocks.infrastructure.scheduling

import com.stablecoin.custody.fireblocks.domain.allocation.FundAllocationService
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.temporal.ChronoUnit

@Component
class AllocationCleanupJob(
    private val fundAllocationService: FundAllocationService,
) {
    @Scheduled(fixedDelayString = "\${custody.polling.interval:120000}")
    @SchedulerLock(name = "allocationCleanup", lockAtLeastFor = "30s", lockAtMostFor = "5m")
    fun cleanupOrphanedAllocations() {
        val cutoff = Instant.now().minus(5, ChronoUnit.MINUTES)
        fundAllocationService.releaseOrphanedLocked(cutoff, 50)
        fundAllocationService.failOrphanedPending(cutoff, 50)
    }
}
