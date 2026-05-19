package com.stablecoin.custody.fireblocks.infrastructure.temporal

import com.stablecoin.custody.fireblocks.domain.port.TransactionStatusSignal
import com.stablecoin.custody.fireblocks.domain.port.WorkflowSignalPort
import com.stablecoin.custody.fireblocks.domain.shared.logger
import com.stablecoin.custody.fireblocks.infrastructure.temporal.dto.toTemporalSignal
import io.temporal.client.WorkflowClient
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

private val log = logger<TemporalWorkflowSignalAdapter>()

@Component
@ConditionalOnProperty(prefix = "temporal", name = ["enabled"], havingValue = "true")
class TemporalWorkflowSignalAdapter(
    private val workflowClient: WorkflowClient,
) : WorkflowSignalPort {
    override fun signalTransactionStatus(
        externalTxId: String,
        signal: TransactionStatusSignal,
    ) {
        val workflowId = "${TemporalConstants.WORKFLOW_ID_PREFIX}$externalTxId"
        log.info("Signaling workflow: workflowId={}, fireblocksStatus={}", workflowId, signal.fireblocksStatus)

        val workflow =
            workflowClient.newWorkflowStub(
                TransactionLifecycleWorkflow::class.java,
                workflowId,
            )
        workflow.onStatusUpdate(signal.toTemporalSignal())
    }
}
