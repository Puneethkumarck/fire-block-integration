package com.stablecoin.custody.fireblocks.infrastructure.temporal

import io.temporal.client.WorkflowClient
import io.temporal.common.converter.DataConverter
import io.temporal.serviceclient.WorkflowServiceStubs
import io.temporal.worker.WorkerFactory
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.runner.ApplicationContextRunner

class TemporalConfigTest {
    private val contextRunner =
        ApplicationContextRunner()
            .withUserConfiguration(TemporalConfig::class.java)

    @Test
    fun `should create Temporal beans when enabled is true`() {
        // given
        val runner =
            contextRunner.withPropertyValues(
                "temporal.enabled=true",
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
    fun `should not create Temporal beans when enabled is false`() {
        // given
        val runner =
            contextRunner.withPropertyValues(
                "temporal.enabled=false",
                "temporal.service-address=localhost:7233",
                "temporal.namespace=custody-fireblocks",
            )

        // when/then
        runner.run { context ->
            assertThat(context).doesNotHaveBean(WorkflowServiceStubs::class.java)
            assertThat(context).doesNotHaveBean(WorkflowClient::class.java)
            assertThat(context).doesNotHaveBean(WorkerFactory::class.java)
            assertThat(context).doesNotHaveBean(DataConverter::class.java)
        }
    }

    @Test
    fun `should not create Temporal beans when enabled is not configured`() {
        // given
        val runner =
            contextRunner.withPropertyValues(
                "temporal.service-address=localhost:7233",
                "temporal.namespace=custody-fireblocks",
            )

        // when/then
        runner.run { context ->
            assertThat(context).doesNotHaveBean(WorkflowServiceStubs::class.java)
            assertThat(context).doesNotHaveBean(WorkflowClient::class.java)
            assertThat(context).doesNotHaveBean(WorkerFactory::class.java)
            assertThat(context).doesNotHaveBean(DataConverter::class.java)
        }
    }
}
