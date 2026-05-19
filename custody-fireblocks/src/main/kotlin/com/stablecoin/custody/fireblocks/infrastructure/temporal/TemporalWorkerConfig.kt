package com.stablecoin.custody.fireblocks.infrastructure.temporal

import io.temporal.worker.Worker
import io.temporal.worker.WorkerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@ConditionalOnProperty(prefix = "temporal", name = ["enabled"], havingValue = "true")
class TemporalWorkerConfig(
    private val factory: WorkerFactory,
    private val activities: TransactionLifecycleActivities,
) {
    @Bean
    fun transactionLifecycleWorker(): Worker {
        val worker = factory.newWorker(TemporalConstants.TASK_QUEUE)
        worker.registerActivitiesImplementations(activities)
        return worker
    }

    @Bean(initMethod = "start", destroyMethod = "shutdown")
    fun workerFactoryStarter(transactionLifecycleWorker: Worker): WorkerFactory = factory
}
