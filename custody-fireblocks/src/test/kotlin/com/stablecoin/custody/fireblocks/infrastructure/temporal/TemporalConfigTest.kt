package com.stablecoin.custody.fireblocks.infrastructure.temporal

import com.fasterxml.jackson.databind.ObjectMapper
import io.temporal.client.WorkflowClient
import io.temporal.common.converter.DataConverter
import io.temporal.serviceclient.WorkflowServiceStubs
import io.temporal.worker.WorkerFactory
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

class TemporalConfigTest {
    @Configuration
    class ObjectMapperConfig {
        @Bean
        fun objectMapper(): ObjectMapper = ObjectMapper().findAndRegisterModules()
    }

    private val contextRunner =
        ApplicationContextRunner()
            .withUserConfiguration(ObjectMapperConfig::class.java, TemporalConfig::class.java)

    @Test
    fun `should create Temporal beans when service-address is configured`() {
        // given
        val runner =
            contextRunner.withPropertyValues(
                "temporal.service-address=localhost:7233",
                "temporal.namespace=custody-fireblocks",
            )

        // when/then
        runner.run { context ->
            assertThat(context).hasSingleBean(WorkflowServiceStubs::class.java)
            assertThat(context).hasSingleBean(WorkflowClient::class.java)
            assertThat(context).hasSingleBean(WorkerFactory::class.java)
            assertThat(context).hasSingleBean(DataConverter::class.java)
            assertThat(context).hasSingleBean(TemporalProperties::class.java)
        }
    }

    @Test
    fun `should not create Temporal beans when service-address is not configured`() {
        // when/then
        contextRunner.run { context ->
            assertThat(context).doesNotHaveBean(WorkflowServiceStubs::class.java)
            assertThat(context).doesNotHaveBean(WorkflowClient::class.java)
            assertThat(context).doesNotHaveBean(WorkerFactory::class.java)
            assertThat(context).doesNotHaveBean(DataConverter::class.java)
        }
    }
}
