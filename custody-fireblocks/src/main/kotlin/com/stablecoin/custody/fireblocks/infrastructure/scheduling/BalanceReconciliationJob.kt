package com.stablecoin.custody.fireblocks.infrastructure.scheduling

import com.stablecoin.custody.fireblocks.domain.port.FireblocksBalancePort
import com.stablecoin.custody.fireblocks.domain.reconciliation.BalanceReconciliationService
import com.stablecoin.custody.fireblocks.domain.reconciliation.InternalBalanceRepository
import com.stablecoin.custody.fireblocks.domain.shared.logger
import com.stablecoin.custody.fireblocks.domain.vault.VaultRepository
import com.stablecoin.custody.fireblocks.domain.wallet.WalletAsset
import com.stablecoin.custody.fireblocks.domain.wallet.WalletAssetRepository
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.UUID

private val log = logger<BalanceReconciliationJob>()

@Component
class BalanceReconciliationJob(
    private val reconciliationService: BalanceReconciliationService,
    private val vaultRepository: VaultRepository,
    private val walletAssetRepository: WalletAssetRepository,
    private val internalBalanceRepository: InternalBalanceRepository,
    private val fireblocksBalancePort: FireblocksBalancePort,
    private val properties: ReconciliationProperties,
) {
    @Scheduled(fixedDelayString = "\${custody.reconciliation.interval:900000}")
    @SchedulerLock(name = "balanceReconciliation", lockAtLeastFor = "30s", lockAtMostFor = "10m")
    fun reconcileBalances() {
        if (!properties.enabled) {
            log.debug("Balance reconciliation is disabled, skipping")
            return
        }

        val activeVaults = vaultRepository.findAllActive()
        log.info("Starting balance reconciliation for {} active vaults", activeVaults.size)

        for (vault in activeVaults) {
            val fireblocksVaultId = vault.fireblocksVaultId
            if (fireblocksVaultId == null) {
                log.warn("Active vault {} has no fireblocksVaultId, skipping", vault.id.value)
                continue
            }

            val walletAssets = walletAssetRepository.findByVaultId(vault.id)

            for (asset in walletAssets) {
                try {
                    reconcileAsset(vault.id.value, fireblocksVaultId, asset)
                } catch (e: Exception) {
                    log.error(
                        "Reconciliation failed for vault={} asset={}/{}",
                        vault.id.value,
                        asset.currency,
                        asset.protocol,
                        e,
                    )
                }
            }
        }

        log.info("Balance reconciliation completed")
    }

    private fun reconcileAsset(
        vaultId: UUID,
        fireblocksVaultId: String,
        asset: WalletAsset,
    ) {
        val tolerance = properties.tolerances.getOrDefault(asset.currency, properties.defaultTolerance)

        val existing =
            internalBalanceRepository.findByVaultIdAndCurrencyAndProtocol(
                vaultId,
                asset.currency,
                asset.protocol,
            )

        if (existing == null) {
            try {
                val balanceResult = fireblocksBalancePort.getBalance(fireblocksVaultId, asset.fireblocksAssetId, true)
                reconciliationService.seedAndPersist(vaultId, asset.currency, asset.protocol, balanceResult.available, tolerance)
            } catch (e: Exception) {
                log.error(
                    "Failed to seed internal balance for vault={} asset={}/{}",
                    vaultId,
                    asset.currency,
                    asset.protocol,
                    e,
                )
            }
            return
        }

        val result =
            try {
                val balanceResult = fireblocksBalancePort.getBalance(fireblocksVaultId, asset.fireblocksAssetId, true)
                reconciliationService.reconcile(existing, balanceResult.available, tolerance)
            } catch (e: Exception) {
                log.warn(
                    "Fireblocks balance unavailable for vault={} asset={}/{}, creating PARTIAL result",
                    vaultId,
                    asset.currency,
                    asset.protocol,
                    e,
                )
                reconciliationService.createPartialResult(
                    vaultId,
                    asset.currency,
                    asset.protocol,
                    existing.balance,
                    tolerance,
                )
            }

        reconciliationService.persistResult(result)
    }
}
