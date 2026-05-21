package com.stablecoin.custody.fireblocks.domain.transaction

import com.stablecoin.custody.fireblocks.domain.allocation.AllocationStatus
import com.stablecoin.custody.fireblocks.domain.allocation.FundAllocationService
import com.stablecoin.custody.fireblocks.domain.audit.AuditLog
import com.stablecoin.custody.fireblocks.domain.audit.AuditLogRepository
import com.stablecoin.custody.fireblocks.domain.audit.AuditOperation
import com.stablecoin.custody.fireblocks.domain.audit.AuditStatus
import com.stablecoin.custody.fireblocks.domain.event.TransactionStatusChangedEvent
import com.stablecoin.custody.fireblocks.domain.exception.AssetNotFoundException
import com.stablecoin.custody.fireblocks.domain.port.EventPublisher
import com.stablecoin.custody.fireblocks.domain.port.FireblocksSubmitCommand
import com.stablecoin.custody.fireblocks.domain.port.FireblocksTransactionPort
import com.stablecoin.custody.fireblocks.domain.shared.logger
import com.stablecoin.custody.fireblocks.domain.vault.VaultId
import com.stablecoin.custody.fireblocks.domain.vault.VaultQueryService
import com.stablecoin.custody.fireblocks.domain.wallet.SupportedAssetRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

private val log = logger<TransactionSubmissionHandler>()

@Service
class TransactionSubmissionHandler(
    private val transactionRepository: TransactionRepository,
    private val supportedAssetRepository: SupportedAssetRepository,
    private val vaultQueryService: VaultQueryService,
    private val fireblocksClient: FireblocksTransactionPort,
    private val eventPublisher: EventPublisher<TransactionStatusChangedEvent>,
    private val auditLogRepository: AuditLogRepository,
    private val fundAllocationService: FundAllocationService,
) {
    @Transactional
    fun handle(command: SubmitTransactionCommand): Transaction {
        log.info("Submitting transaction: externalTxId={}", command.externalTxId)

        transactionRepository.findByExternalTxId(command.externalTxId)?.let {
            log.info("Transaction already exists: externalTxId={}", command.externalTxId)
            return it
        }

        val vault = vaultQueryService.getVault(VaultId(UUID.fromString(command.sourceVaultId)))
        vault.assertActive()

        val supportedAsset =
            supportedAssetRepository.findByCurrencyAndProtocol(command.currency, command.protocol)
                ?: throw AssetNotFoundException(command.sourceVaultId, "${command.currency}/${command.protocol}")

        val transaction = Transaction.create(command, supportedAsset.fireblocksAssetId)
        val saved = transactionRepository.save(transaction)

        val fireblocksVaultId =
            vault.fireblocksVaultId
                ?: throw IllegalStateException("Active vault ${vault.id} has no fireblocksVaultId")

        val allocation =
            fundAllocationService.createAndLock(
                allocationId = "alloc-${command.externalTxId}",
                vaultId = vault.id.value,
                fireblocksVaultId = fireblocksVaultId,
                assetId = supportedAsset.fireblocksAssetId,
                currency = command.currency,
                protocol = command.protocol,
                amount = command.amount,
            )

        if (allocation.status == AllocationStatus.FAILED) {
            log.warn("Allocation lock failed, marking transaction as FAILED: externalTxId={}", command.externalTxId)

            val failed = saved.markFailed()
            val result = transactionRepository.save(failed)

            auditLogRepository.save(
                AuditLog.create(
                    operation = AuditOperation.TRANSACTION_SUBMISSION_FAILED,
                    actor = "system",
                    resourceId = result.id.value.toString(),
                    status = AuditStatus.FAILURE,
                    details = mapOf("error" to "Allocation lock failed"),
                ),
            )

            return result
        }

        val fireblocksResult =
            try {
                fireblocksClient.submitTransaction(
                    FireblocksSubmitCommand(
                        externalTxId = command.externalTxId,
                        sourceVaultId = fireblocksVaultId,
                        destinationAddress = command.destinationAddress,
                        assetId = supportedAsset.fireblocksAssetId,
                        amount = command.amount,
                        feeLevel = command.feeLevel ?: FeeLevel.MEDIUM,
                        treatAsGrossAmount = command.treatAsGrossAmount ?: false,
                        note = command.note,
                    ),
                )
            } catch (e: Exception) {
                log.error("Fireblocks transaction submission failed: externalTxId={}", command.externalTxId, e)

                try {
                    fundAllocationService.release(allocation.allocationId)
                } catch (releaseEx: Exception) {
                    log.error("Failed to release allocation during submission failure: allocationId={}", allocation.allocationId, releaseEx)
                }

                val failed = saved.markFailed()
                val result = transactionRepository.save(failed)

                auditLogRepository.save(
                    AuditLog.create(
                        operation = AuditOperation.TRANSACTION_SUBMISSION_FAILED,
                        actor = "system",
                        resourceId = result.id.value.toString(),
                        status = AuditStatus.FAILURE,
                        details = mapOf("error" to (e.message ?: "unknown")),
                    ),
                )

                return result
            }

        val submitted = saved.markSubmitted(fireblocksResult.id)
        val result = transactionRepository.save(submitted)

        fundAllocationService.consume(allocation.allocationId, result.id.value)

        eventPublisher.publish(
            TransactionStatusChangedEvent(
                transactionId = result.id.value,
                externalTxId = result.externalTxId,
                previousStatus = TransactionStatus.CREATED.name,
                newStatus = TransactionStatus.SUBMITTED.name,
                fireblocksStatus = fireblocksResult.status,
                subStatus = fireblocksResult.subStatus,
                txHash = fireblocksResult.txHash,
                occurredAt = Instant.now(),
            ),
        )

        auditLogRepository.save(
            AuditLog.create(
                operation = AuditOperation.TRANSACTION_SUBMITTED,
                actor = "system",
                resourceId = result.id.value.toString(),
                status = AuditStatus.SUCCESS,
                details = mapOf("fireblocksTransactionId" to fireblocksResult.id),
            ),
        )

        return result
    }
}
