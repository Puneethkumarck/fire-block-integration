package com.stablecoin.custody.fireblocks.infrastructure.temporal

import com.stablecoin.custody.fireblocks.infrastructure.temporal.dto.TemporalTransactionStatusSignal
import com.stablecoin.custody.fireblocks.test.fixtures.aTransactionStatusSignal
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import io.temporal.client.WorkflowClient
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class TemporalWorkflowSignalAdapterTest {
    private val workflowClient = mockk<WorkflowClient>()
    private val adapter = TemporalWorkflowSignalAdapter(workflowClient)

    @Test
    fun `should signal workflow with correct workflow ID`() {
        // given
        val externalTxId = "ext-tx-001"
        val signal = aTransactionStatusSignal(fireblocksStatus = "COMPLETED", txHash = "0xhash")
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
        assertThat(signalSlot.captured.fireblocksStatus).isEqualTo("COMPLETED")
        assertThat(signalSlot.captured.txHash).isEqualTo("0xhash")
    }
}
