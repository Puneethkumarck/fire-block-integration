package com.stablecoin.custody.fireblocks.infrastructure.temporal

import com.stablecoin.custody.fireblocks.domain.audit.AuditLog
import com.stablecoin.custody.fireblocks.domain.audit.AuditLogRepository
import com.stablecoin.custody.fireblocks.domain.audit.AuditOperation
import com.stablecoin.custody.fireblocks.domain.audit.AuditStatus
import com.stablecoin.custody.fireblocks.domain.event.TransactionStatusChangedEvent
import com.stablecoin.custody.fireblocks.domain.port.EventPublisher
import com.stablecoin.custody.fireblocks.domain.port.FireblocksSubmitCommand
import com.stablecoin.custody.fireblocks.domain.port.FireblocksTransactionPort
import com.stablecoin.custody.fireblocks.domain.shared.logger
import com.stablecoin.custody.fireblocks.domain.transaction.FeeLevel
import com.stablecoin.custody.fireblocks.domain.transaction.SubmitTransactionCommand
import com.stablecoin.custody.fireblocks.domain.transaction.Transaction
import com.stablecoin.custody.fireblocks.domain.transaction.TransactionId
import com.stablecoin.custody.fireblocks.domain.transaction.TransactionRepository
import com.stablecoin.custody.fireblocks.domain.transaction.TransactionStatus
import com.stablecoin.custody.fireblocks.infrastructure.temporal.dto.CreateTransactionResult
import com.stablecoin.custody.fireblocks.infrastructure.temporal.dto.FireblocksSubmitActivityCommand
import com.stablecoin.custody.fireblocks.infrastructure.temporal.dto.PollStatusResult
import com.stablecoin.custody.fireblocks.infrastructure.temporal.dto.ReleaseFundsActivityCommand
import com.stablecoin.custody.fireblocks.infrastructure.temporal.dto.ReserveFundsActivityCommand
import com.stablecoin.custody.fireblocks.infrastructure.temporal.dto.ReserveFundsResult
import com.stablecoin.custody.fireblocks.infrastructure.temporal.dto.StartTransactionRequest
import com.stablecoin.custody.fireblocks.infrastructure.temporal.dto.SubmitResult
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

private val log = logger<TransactionLifecycleActivitiesImpl>()

@Component
class TransactionLifecycleActivitiesImpl(
    private val transactionRepository: TransactionRepository,
    private val fireblocksTransactionPort: FireblocksTransactionPort,
    private val eventPublisher: EventPublisher<TransactionStatusChangedEvent>,
    private val auditLogRepository: AuditLogRepository,
) : TransactionLifecycleActivities {
    @Transactional
    override fun createTransaction(request: StartTransactionRequest): CreateTransactionResult {
        log.info("Creating transaction: externalTxId={}", request.externalTxId)

        transactionRepository.findByExternalTxId(request.externalTxId)?.let {
            log.info("Transaction already exists: externalTxId={}", request.externalTxId)
            return CreateTransactionResult(transactionId = it.id.value.toString())
        }

        val command = request.toSubmitTransactionCommand()
        val transaction = Transaction.create(command, request.fireblocksAssetId)
        val saved = transactionRepository.save(transaction)

        eventPublisher.publish(
            TransactionStatusChangedEvent(
                transactionId = saved.id.value,
                externalTxId = saved.externalTxId,
                previousStatus = TransactionStatus.CREATED.name,
                newStatus = TransactionStatus.CREATED.name,
                fireblocksStatus = null,
                subStatus = null,
                txHash = null,
                occurredAt = Instant.now(),
            ),
        )

        auditLogRepository.save(
            AuditLog.create(
                operation = AuditOperation.TRANSACTION_CREATED,
                actor = "temporal-activity",
                resourceId = saved.id.value.toString(),
                status = AuditStatus.SUCCESS,
                details = mapOf("externalTxId" to request.externalTxId),
            ),
        )

        log.info("Transaction created: id={}, externalTxId={}", saved.id.value, saved.externalTxId)
        return CreateTransactionResult(transactionId = saved.id.value.toString())
    }

    @Transactional
    override fun recordSubmission(
        transactionId: String,
        fireblocksTransactionId: String,
    ) {
        log.info("Recording submission: transactionId={}, fireblocksTransactionId={}", transactionId, fireblocksTransactionId)

        val transaction = loadTransaction(transactionId)
        val previousStatus = transaction.status
        val submitted = transaction.markSubmitted(fireblocksTransactionId)
        val saved = transactionRepository.save(submitted)

        eventPublisher.publish(
            TransactionStatusChangedEvent(
                transactionId = saved.id.value,
                externalTxId = saved.externalTxId,
                previousStatus = previousStatus.name,
                newStatus = TransactionStatus.SUBMITTED.name,
                fireblocksStatus = null,
                subStatus = null,
                txHash = null,
                occurredAt = Instant.now(),
            ),
        )

        auditLogRepository.save(
            AuditLog.create(
                operation = AuditOperation.TRANSACTION_SUBMITTED,
                actor = "temporal-activity",
                resourceId = saved.id.value.toString(),
                status = AuditStatus.SUCCESS,
                details = mapOf("fireblocksTransactionId" to fireblocksTransactionId),
            ),
        )

        log.info("Submission recorded: transactionId={}", transactionId)
    }

    @Transactional
    override fun recordStatusChange(
        transactionId: String,
        newStatus: String,
        fireblocksStatus: String,
        subStatus: String?,
        txHash: String?,
    ) {
        log.info("Recording status change: transactionId={}, newStatus={}", transactionId, newStatus)

        val transaction = loadTransaction(transactionId)
        val status = TransactionStatus.valueOf(newStatus)
        val previousStatus = transaction.status

        val updated = transaction.updateStatus(status, fireblocksStatus, subStatus, txHash)
        val saved = transactionRepository.save(updated)

        eventPublisher.publish(
            TransactionStatusChangedEvent(
                transactionId = saved.id.value,
                externalTxId = saved.externalTxId,
                previousStatus = previousStatus.name,
                newStatus = status.name,
                fireblocksStatus = fireblocksStatus,
                subStatus = subStatus,
                txHash = txHash,
                occurredAt = Instant.now(),
            ),
        )

        auditLogRepository.save(
            AuditLog.create(
                operation = AuditOperation.TRANSACTION_STATUS_UPDATED,
                actor = "temporal-activity",
                resourceId = saved.id.value.toString(),
                status = AuditStatus.SUCCESS,
                details =
                    mapOf(
                        "previousStatus" to previousStatus.name,
                        "newStatus" to status.name,
                        "fireblocksStatus" to fireblocksStatus,
                    ),
            ),
        )

        log.info("Status change recorded: transactionId={}, {} -> {}", transactionId, previousStatus, status)
    }

    @Transactional
    override fun completeTransaction(
        transactionId: String,
        newStatus: String,
        fireblocksStatus: String,
        subStatus: String?,
        txHash: String?,
    ) {
        log.info("Completing transaction: transactionId={}, newStatus={}", transactionId, newStatus)

        val transaction = loadTransaction(transactionId)
        val status = TransactionStatus.valueOf(newStatus)
        val previousStatus = transaction.status

        val updated = transaction.updateStatus(status, fireblocksStatus, subStatus, txHash)
        val saved = transactionRepository.save(updated)

        eventPublisher.publish(
            TransactionStatusChangedEvent(
                transactionId = saved.id.value,
                externalTxId = saved.externalTxId,
                previousStatus = previousStatus.name,
                newStatus = status.name,
                fireblocksStatus = fireblocksStatus,
                subStatus = subStatus,
                txHash = txHash,
                occurredAt = Instant.now(),
            ),
        )

        auditLogRepository.save(
            AuditLog.create(
                operation = AuditOperation.TRANSACTION_STATUS_UPDATED,
                actor = "temporal-activity",
                resourceId = saved.id.value.toString(),
                status = AuditStatus.SUCCESS,
                details =
                    mapOf(
                        "previousStatus" to previousStatus.name,
                        "newStatus" to status.name,
                        "fireblocksStatus" to fireblocksStatus,
                    ),
            ),
        )

        log.info("Transaction completed: transactionId={}, {} -> {}", transactionId, previousStatus, status)
    }

    override fun submitToFireblocks(command: FireblocksSubmitActivityCommand): SubmitResult {
        log.info("Submitting to Fireblocks: externalTxId={}", command.externalTxId)

        val result = fireblocksTransactionPort.submitTransaction(command.toFireblocksSubmitCommand())

        log.info("Fireblocks submission result: fireblocksTransactionId={}, status={}", result.id, result.status)
        return SubmitResult(
            fireblocksTransactionId = result.id,
            status = result.status,
            subStatus = result.subStatus,
            txHash = result.txHash,
        )
    }

    override fun fetchTransactionStatus(fireblocksTransactionId: String): PollStatusResult {
        log.info("Fetching transaction status: fireblocksTransactionId={}", fireblocksTransactionId)

        val result = fireblocksTransactionPort.getTransaction(fireblocksTransactionId)

        log.info("Fetched status: fireblocksTransactionId={}, status={}", fireblocksTransactionId, result.status)
        return PollStatusResult(
            fireblocksStatus = result.status,
            subStatus = result.subStatus,
            txHash = result.txHash,
        )
    }

    override fun reserveFunds(command: ReserveFundsActivityCommand): ReserveFundsResult =
        throw UnsupportedOperationException("Fund allocation not yet implemented")

    override fun releaseFunds(command: ReleaseFundsActivityCommand): Unit =
        throw UnsupportedOperationException("Fund allocation not yet implemented")

    private fun loadTransaction(transactionId: String): Transaction =
        transactionRepository.findById(TransactionId(UUID.fromString(transactionId)))
            ?: throw IllegalArgumentException("Transaction not found: $transactionId")

    private fun StartTransactionRequest.toSubmitTransactionCommand() =
        SubmitTransactionCommand(
            externalTxId = externalTxId,
            sourceVaultId = fireblocksVaultId,
            destinationAddress = destinationAddress,
            currency = currency,
            protocol = protocol,
            amount = amount,
            feeLevel = FeeLevel.valueOf(feeLevel),
            treatAsGrossAmount = treatAsGrossAmount,
            note = note,
        )

    private fun FireblocksSubmitActivityCommand.toFireblocksSubmitCommand() =
        FireblocksSubmitCommand(
            externalTxId = externalTxId,
            sourceVaultId = sourceVaultId,
            destinationAddress = destinationAddress,
            assetId = assetId,
            amount = amount,
            feeLevel = FeeLevel.valueOf(feeLevel),
            treatAsGrossAmount = treatAsGrossAmount,
            note = note,
        )
}
