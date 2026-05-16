package com.stablecoin.custody.fireblocks.infrastructure.scheduling

import com.stablecoin.custody.fireblocks.domain.port.FireblocksVaultPort
import com.stablecoin.custody.fireblocks.domain.shared.logger
import com.stablecoin.custody.fireblocks.domain.vault.VaultRepository
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.temporal.ChronoUnit

private val log = logger<VaultRecoveryJob>()

@Component
class VaultRecoveryJob(
    private val vaultRepository: VaultRepository,
    private val fireblocksVaultPort: FireblocksVaultPort,
) {
    @Scheduled(fixedDelayString = "\${custody.polling.vault-interval:120000}")
    @SchedulerLock(name = "vaultRecovery", lockAtLeastFor = "30s", lockAtMostFor = "90s")
    fun recoverPendingVaults() {
        val cutoff = Instant.now().minus(5, ChronoUnit.MINUTES)
        val pendingVaults = vaultRepository.findPendingOlderThan(cutoff)

        if (pendingVaults.isEmpty()) {
            return
        }

        log.info("Recovering {} pending vaults", pendingVaults.size)

        pendingVaults.forEach { vault ->
            try {
                val result = fireblocksVaultPort.createVault(vault.name, vault.customerRefId)
                val activated = vault.activate(result.id)
                vaultRepository.save(activated)
                log.info("Recovered vault: id={}, fireblocksVaultId={}", vault.id.value, result.id)
            } catch (e: Exception) {
                log.error("Failed to recover vault: id={}", vault.id.value, e)
                val failed = vault.markFailed()
                vaultRepository.save(failed)
            }
        }
    }
}
