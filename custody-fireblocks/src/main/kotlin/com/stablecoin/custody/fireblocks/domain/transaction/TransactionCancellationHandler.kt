package com.stablecoin.custody.fireblocks.domain.transaction

import com.stablecoin.custody.fireblocks.domain.audit.AuditLog
import com.stablecoin.custody.fireblocks.domain.audit.AuditLogRepository
import com.stablecoin.custody.fireblocks.domain.audit.AuditOperation
import com.stablecoin.custody.fireblocks.domain.audit.AuditStatus
import com.stablecoin.custody.fireblocks.domain.exception.InvalidTransactionStateException
import com.stablecoin.custody.fireblocks.domain.exception.TransactionAlreadyTerminalException
import com.stablecoin.custody.fireblocks.domain.exception.TransactionNotCancellableException
import com.stablecoin.custody.fireblocks.domain.exception.TransactionNotFoundException
import com.stablecoin.custody.fireblocks.domain.port.FireblocksTransactionPort
import com.stablecoin.custody.fireblocks.domain.shared.logger
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

private val log = logger<TransactionCancellationHandler>()

@Service
class TransactionCancellationHandler(
    private val transactionRepository: TransactionRepository,
    private val fireblocksTransactionPort: FireblocksTransactionPort,
    private val auditLogRepository: AuditLogRepository,
) {
    @Transactional
    fun handle(transactionId: TransactionId): Transaction {
        log.info("Cancellation requested: transactionId={}", transactionId.value)

        val transaction =
            transactionRepository.findById(transactionId)
                ?: throw TransactionNotFoundException(transactionId.value.toString())

        if (transaction.status.terminal) {
            throw TransactionAlreadyTerminalException(transactionId.value.toString(), transaction.status.name)
        }

        val fireblocksTxId =
            transaction.fireblocksTransactionId
                ?: throw InvalidTransactionStateException(
                    transactionId.value.toString(),
                    transaction.status.name,
                    "CANCELLING",
                )

        val success = fireblocksTransactionPort.cancelTransaction(fireblocksTxId)

        if (!success) {
            auditLogRepository.save(
                AuditLog.create(
                    operation = AuditOperation.TRANSACTION_CANCELLATION_REJECTED,
                    actor = "system",
                    resourceId = transactionId.value.toString(),
                    status = AuditStatus.FAILURE,
                    details = mapOf("fireblocksTransactionId" to fireblocksTxId),
                ),
            )
            throw TransactionNotCancellableException(transactionId.value.toString())
        }

        auditLogRepository.save(
            AuditLog.create(
                operation = AuditOperation.TRANSACTION_CANCELLATION_REQUESTED,
                actor = "system",
                resourceId = transactionId.value.toString(),
                status = AuditStatus.SUCCESS,
                details = mapOf("fireblocksTransactionId" to fireblocksTxId),
            ),
        )

        return transaction
    }
}
