package com.stablecoin.custody.fireblocks.infrastructure.temporal

import com.fasterxml.jackson.databind.ObjectMapper
import io.temporal.client.WorkflowClient
import io.temporal.client.WorkflowClientOptions
import io.temporal.common.converter.DataConverter
import io.temporal.common.converter.DefaultDataConverter
import io.temporal.common.converter.JacksonJsonPayloadConverter
import io.temporal.serviceclient.WorkflowServiceStubs
import io.temporal.serviceclient.WorkflowServiceStubsOptions
import io.temporal.worker.WorkerFactory
import io.temporal.worker.WorkerFactoryOptions
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@ConditionalOnProperty(prefix = "temporal", name = ["enabled"], havingValue = "true")
@EnableConfigurationProperties(TemporalProperties::class)
class TemporalConfig(
    private val properties: TemporalProperties,
) {
    @Bean
    fun temporalDataConverter(): DataConverter {
        val objectMapper = ObjectMapper().findAndRegisterModules()
        return DefaultDataConverter
            .newDefaultInstance()
            .withPayloadConverterOverrides(JacksonJsonPayloadConverter(objectMapper))
    }

    @Bean(destroyMethod = "shutdown")
    fun workflowServiceStubs(): WorkflowServiceStubs =
        WorkflowServiceStubs.newServiceStubs(
            WorkflowServiceStubsOptions
                .newBuilder()
                .setTarget(properties.serviceAddress)
                .build(),
        )

    @Bean
    fun workflowClient(
        workflowServiceStubs: WorkflowServiceStubs,
        temporalDataConverter: DataConverter,
    ): WorkflowClient =
        WorkflowClient.newInstance(
            workflowServiceStubs,
            WorkflowClientOptions
                .newBuilder()
                .setNamespace(properties.namespace)
                .setDataConverter(temporalDataConverter)
                .build(),
        )

    @Bean(destroyMethod = "shutdown")
    fun workerFactory(workflowClient: WorkflowClient): WorkerFactory =
        WorkerFactory.newInstance(
            workflowClient,
            WorkerFactoryOptions.newBuilder().build(),
        )
}
