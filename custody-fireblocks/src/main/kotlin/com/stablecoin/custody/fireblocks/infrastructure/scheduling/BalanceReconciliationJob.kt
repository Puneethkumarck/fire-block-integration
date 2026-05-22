package com.stablecoin.custody.fireblocks.infrastructure.scheduling

import com.stablecoin.custody.fireblocks.domain.audit.AuditLog
import com.stablecoin.custody.fireblocks.domain.audit.AuditLogRepository
import com.stablecoin.custody.fireblocks.domain.audit.AuditOperation
import com.stablecoin.custody.fireblocks.domain.audit.AuditStatus
import com.stablecoin.custody.fireblocks.domain.event.ReconciliationBreakDetectedEvent
import com.stablecoin.custody.fireblocks.domain.port.EventPublisher
import com.stablecoin.custody.fireblocks.domain.port.FireblocksBalancePort
import com.stablecoin.custody.fireblocks.domain.reconciliation.BalanceReconciliationService
import com.stablecoin.custody.fireblocks.domain.reconciliation.InternalBalance
import com.stablecoin.custody.fireblocks.domain.reconciliation.InternalBalanceRepository
import com.stablecoin.custody.fireblocks.domain.reconciliation.ReconciliationResult
import com.stablecoin.custody.fireblocks.domain.reconciliation.ReconciliationResultRepository
import com.stablecoin.custody.fireblocks.domain.reconciliation.ReconciliationStatus
import com.stablecoin.custody.fireblocks.domain.shared.logger
import com.stablecoin.custody.fireblocks.domain.vault.VaultRepository
import com.stablecoin.custody.fireblocks.domain.wallet.WalletAsset
import com.stablecoin.custody.fireblocks.domain.wallet.WalletAssetRepository
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

private val log = logger<BalanceReconciliationJob>()

@Component
class BalanceReconciliationJob(
    private val reconciliationService: BalanceReconciliationService,
    private val vaultRepository: VaultRepository,
    private val walletAssetRepository: WalletAssetRepository,
    private val internalBalanceRepository: InternalBalanceRepository,
    private val reconciliationResultRepository: ReconciliationResultRepository,
    private val fireblocksBalancePort: FireblocksBalancePort,
    private val auditLogRepository: AuditLogRepository,
    private val breakEventPublisher: EventPublisher<ReconciliationBreakDetectedEvent>,
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
            val walletAssets = walletAssetRepository.findByVaultId(vault.id)

            for (asset in walletAssets) {
                try {
                    reconcileAsset(vault.id.value, vault.fireblocksVaultId!!, asset)
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
            seedInternalBalance(vaultId, fireblocksVaultId, asset, tolerance)
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

        reconciliationResultRepository.save(result)
        auditReconciliation(result)

        if (result.status == ReconciliationStatus.MISMATCHED) {
            publishBreakEvent(result)
        }
    }

    private fun seedInternalBalance(
        vaultId: UUID,
        fireblocksVaultId: String,
        asset: WalletAsset,
        tolerance: BigDecimal,
    ) {
        try {
            val balanceResult = fireblocksBalancePort.getBalance(fireblocksVaultId, asset.fireblocksAssetId, true)
            val seeded =
                InternalBalance(
                    id = UUID.randomUUID(),
                    vaultId = vaultId,
                    currency = asset.currency,
                    protocol = asset.protocol,
                    balance = balanceResult.available,
                    lastTransactionId = null,
                    updatedAt = Instant.now(),
                )
            internalBalanceRepository.save(seeded)

            val matchedResult = reconciliationService.reconcile(seeded, balanceResult.available, tolerance)
            reconciliationResultRepository.save(matchedResult)
            auditReconciliation(matchedResult)

            log.info(
                "Seeded internal balance for vault={} asset={}/{} balance={}",
                vaultId,
                asset.currency,
                asset.protocol,
                balanceResult.available,
            )
        } catch (e: Exception) {
            log.error(
                "Failed to seed internal balance for vault={} asset={}/{}",
                vaultId,
                asset.currency,
                asset.protocol,
                e,
            )
        }
    }

    private fun auditReconciliation(result: ReconciliationResult) {
        val operation =
            when (result.status) {
                ReconciliationStatus.MATCHED -> AuditOperation.RECONCILIATION_MATCHED
                ReconciliationStatus.MISMATCHED -> AuditOperation.RECONCILIATION_MISMATCHED
                ReconciliationStatus.PARTIAL -> AuditOperation.RECONCILIATION_PARTIAL
            }

        auditLogRepository.save(
            AuditLog.create(
                operation = operation,
                actor = "system",
                resourceId = result.vaultId.toString(),
                status = if (result.status == ReconciliationStatus.MISMATCHED) AuditStatus.FAILURE else AuditStatus.SUCCESS,
                details =
                    mapOf(
                        "currency" to result.currency,
                        "protocol" to result.protocol,
                        "drift" to result.drift.toPlainString(),
                        "status" to result.status.name,
                    ),
            ),
        )
    }

    private fun publishBreakEvent(result: ReconciliationResult) {
        breakEventPublisher.publish(
            ReconciliationBreakDetectedEvent(
                vaultId = result.vaultId,
                currency = result.currency,
                protocol = result.protocol,
                internalBalance = result.internalBalance,
                fireblocksBalance = result.fireblocksBalance,
                drift = result.drift,
                absoluteDrift = result.absoluteDrift,
                toleranceUsed = result.toleranceUsed,
                occurredAt = result.createdAt,
            ),
        )

        log.warn(
            "Reconciliation break detected: vault={} asset={}/{} drift={}",
            result.vaultId,
            result.currency,
            result.protocol,
            result.drift,
        )
    }
}
