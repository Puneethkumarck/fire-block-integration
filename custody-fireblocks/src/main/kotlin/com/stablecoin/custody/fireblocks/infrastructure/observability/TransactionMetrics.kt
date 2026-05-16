package com.stablecoin.custody.fireblocks.infrastructure.observability

import com.stablecoin.custody.fireblocks.domain.transaction.TransactionRepository
import com.stablecoin.custody.fireblocks.domain.transaction.TransactionStatus
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag
import org.springframework.stereotype.Component

@Component
class TransactionMetrics(
    meterRegistry: MeterRegistry,
    private val transactionRepository: TransactionRepository,
) {
    init {
        TransactionStatus.entries.forEach { status ->
            meterRegistry.gauge(
                "custody.transactions.status",
                listOf(Tag.of("status", status.name)),
                transactionRepository,
            ) { repo -> repo.countByStatus(status).toDouble() }
        }
    }
}
