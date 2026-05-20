package com.stablecoin.custody.fireblocks.infrastructure.temporal

import com.stablecoin.custody.fireblocks.infrastructure.temporal.dto.TemporalTransactionStatusSignal
import com.stablecoin.custody.fireblocks.test.fixtures.aTransactionStatusSignal
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import io.temporal.api.common.v1.WorkflowExecution
import io.temporal.client.WorkflowClient
import io.temporal.client.WorkflowNotFoundException
import io.temporal.client.WorkflowServiceException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class TemporalWorkflowSignalAdapterTest {
    private val workflowClient = mockk<WorkflowClient>()
    private val adapter = TemporalWorkflowSignalAdapter(workflowClient)

    @Test
    fun `should signal workflow with correct workflow ID`() {
        // given
        val externalTxId = "ext-tx-001"
        val signal =
            aTransactionStatusSignal(
                fireblocksTransactionId = "fb-tx-999",
                fireblocksStatus = "COMPLETED",
                txHash = "0xhash",
            )
        val workflowStub = mockk<TransactionLifecycleWorkflow>()
        val signalSlot = slot<TemporalTransactionStatusSignal>()

        every {
            workflowClient.newWorkflowStub(
                TransactionLifecycleWorkflow::class.java,
                "${TemporalConstants.WORKFLOW_ID_PREFIX}$externalTxId",
            )
        } returns workflowStub
        every { workflowStub.onStatusUpdate(capture(signalSlot)) } returns Unit

        // when
        adapter.signalTransactionStatus(externalTxId, signal)

        // then
        verify {
            workflowClient.newWorkflowStub(
                TransactionLifecycleWorkflow::class.java,
                "TransactionLifecycle_ext-tx-001",
            )
        }
        val expectedSignal =
            TemporalTransactionStatusSignal(
                fireblocksTransactionId = "fb-tx-999",
                fireblocksStatus = "COMPLETED",
                subStatus = null,
                txHash = "0xhash",
            )
        assertThat(signalSlot.captured).usingRecursiveComparison().isEqualTo(expectedSignal)
    }

    @Test
    fun `should throw when externalTxId is blank`() {
        // given
        val signal = aTransactionStatusSignal()

        // when / then
        assertThatThrownBy { adapter.signalTransactionStatus("  ", signal) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("externalTxId must not be blank")
    }

    @Test
    fun `should handle WorkflowNotFoundException gracefully`() {
        // given
        val externalTxId = "ext-tx-not-found"
        val signal = aTransactionStatusSignal()
        val workflowStub = mockk<TransactionLifecycleWorkflow>()

        every {
            workflowClient.newWorkflowStub(
                TransactionLifecycleWorkflow::class.java,
                "${TemporalConstants.WORKFLOW_ID_PREFIX}$externalTxId",
            )
        } returns workflowStub
        val execution = WorkflowExecution.getDefaultInstance()
        every { workflowStub.onStatusUpdate(any()) } throws
            WorkflowNotFoundException(execution, "not found", RuntimeException("workflow not found"))

        // when / then — should not throw
        adapter.signalTransactionStatus(externalTxId, signal)
    }

    @Test
    fun `should rethrow WorkflowServiceException`() {
        // given
        val externalTxId = "ext-tx-service-err"
        val signal = aTransactionStatusSignal()
        val workflowStub = mockk<TransactionLifecycleWorkflow>()

        every {
            workflowClient.newWorkflowStub(
                TransactionLifecycleWorkflow::class.java,
                "${TemporalConstants.WORKFLOW_ID_PREFIX}$externalTxId",
            )
        } returns workflowStub
        val execution = WorkflowExecution.getDefaultInstance()
        every { workflowStub.onStatusUpdate(any()) } throws
            WorkflowServiceException(execution, "service error", RuntimeException("connection failed"))

        // when / then
        assertThatThrownBy { adapter.signalTransactionStatus(externalTxId, signal) }
            .isInstanceOf(WorkflowServiceException::class.java)
    }
}
