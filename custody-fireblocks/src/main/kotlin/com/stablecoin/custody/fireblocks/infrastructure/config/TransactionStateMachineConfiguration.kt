package com.stablecoin.custody.fireblocks.infrastructure.config

import com.stablecoin.custody.fireblocks.domain.exception.InvalidTransactionStateException
import com.stablecoin.custody.fireblocks.domain.shared.StateMachine
import com.stablecoin.custody.fireblocks.domain.transaction.Transaction
import com.stablecoin.custody.fireblocks.domain.transaction.TransactionStatus
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class TransactionStateMachineConfiguration {
    @Bean
    fun transactionStateMachine(): StateMachine<TransactionStatus, Transaction> =
        StateMachine(
            mapOf(
                TransactionStatus.CREATED to setOf(TransactionStatus.SUBMITTED, TransactionStatus.FAILED),
                TransactionStatus.SUBMITTED to setOf(TransactionStatus.PROCESSING, TransactionStatus.FAILED),
                TransactionStatus.PROCESSING to setOf(TransactionStatus.CONFIRMING, TransactionStatus.FAILED),
                TransactionStatus.CONFIRMING to setOf(TransactionStatus.CONFIRMED, TransactionStatus.FAILED),
            ),
        ) { entity, target ->
            throw InvalidTransactionStateException(
                entity.id.value.toString(),
                entity.status.name,
                target.name,
            )
        }
}
