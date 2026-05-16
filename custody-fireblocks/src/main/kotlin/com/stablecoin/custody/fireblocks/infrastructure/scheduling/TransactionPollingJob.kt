package com.stablecoin.custody.fireblocks.infrastructure.scheduling

import com.stablecoin.custody.fireblocks.domain.port.FireblocksTransactionPort
import com.stablecoin.custody.fireblocks.domain.shared.logger
import com.stablecoin.custody.fireblocks.domain.transaction.TransactionRepository
import com.stablecoin.custody.fireblocks.domain.transaction.TransactionStatusHandler
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.temporal.ChronoUnit

private val log = logger<TransactionPollingJob>()

@Component
class TransactionPollingJob(
    private val transactionRepository: TransactionRepository,
    private val fireblocksTransactionPort: FireblocksTransactionPort,
    private val transactionStatusHandler: TransactionStatusHandler,
) {
    @Scheduled(fixedDelayString = "\${custody.polling.interval:120000}")
    @SchedulerLock(name = "transactionPolling", lockAtLeastFor = "30s", lockAtMostFor = "90s")
    fun pollStaleTransactions() {
        val cutoff = Instant.now().minus(2, ChronoUnit.MINUTES)
        val staleTransactions = transactionRepository.findStaleNonTerminal(cutoff, 50)

        if (staleTransactions.isEmpty()) {
            return
        }

        log.info("Polling {} stale transactions", staleTransactions.size)

        staleTransactions.forEach { transaction ->
            try {
                val fireblocksTxId = transaction.fireblocksTransactionId ?: return@forEach
                val result = fireblocksTransactionPort.getTransaction(fireblocksTxId)
                transactionStatusHandler.handleStatusUpdate(
                    fireblocksTxId = fireblocksTxId,
                    fireblocksStatus = result.status,
                    subStatus = result.subStatus,
                    txHash = result.txHash,
                )
            } catch (e: Exception) {
                log.error(
                    "Failed to poll transaction: id={}, fireblocksId={}",
                    transaction.id.value,
                    transaction.fireblocksTransactionId,
                    e,
                )
            }
        }
    }

    @Scheduled(fixedDelayString = "\${custody.polling.interval:120000}")
    @SchedulerLock(name = "staleCreatedTransactions", lockAtLeastFor = "30s", lockAtMostFor = "90s")
    fun recoverStaleCreatedTransactions() {
        val cutoff = Instant.now().minus(10, ChronoUnit.MINUTES)
        val staleCreated = transactionRepository.findStaleCreated(cutoff, 50)

        if (staleCreated.isEmpty()) {
            return
        }

        log.info("Recovering {} stale CREATED transactions", staleCreated.size)

        staleCreated.forEach { transaction ->
            try {
                val existing = fireblocksTransactionPort.getByExternalId(transaction.externalTxId)
                if (existing != null) {
                    transactionStatusHandler.handleStatusUpdate(
                        fireblocksTxId = existing.id,
                        fireblocksStatus = existing.status,
                        subStatus = existing.subStatus,
                        txHash = existing.txHash,
                    )
                } else {
                    val failed = transaction.markFailed()
                    transactionRepository.save(failed)
                    log.warn("Marked stale CREATED transaction as FAILED: id={}", transaction.id.value)
                }
            } catch (e: Exception) {
                log.error("Failed to recover CREATED transaction: id={}", transaction.id.value, e)
            }
        }
    }
}
