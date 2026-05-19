package com.stablecoin.custody.fireblocks.infrastructure.temporal

import com.stablecoin.custody.fireblocks.domain.port.TransactionStatusSignal
import com.stablecoin.custody.fireblocks.domain.port.WorkflowSignalPort
import com.stablecoin.custody.fireblocks.domain.shared.logger
import com.stablecoin.custody.fireblocks.infrastructure.temporal.dto.toTemporalSignal
import io.temporal.client.WorkflowClient
import io.temporal.client.WorkflowNotFoundException
import io.temporal.client.WorkflowServiceException
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
        require(externalTxId.isNotBlank()) { "externalTxId must not be blank" }

        val workflowId = "${TemporalConstants.WORKFLOW_ID_PREFIX}$externalTxId"
        log.info("Signaling workflow: workflowId={}, fireblocksStatus={}", workflowId, signal.fireblocksStatus)

        try {
            val workflow =
                workflowClient.newWorkflowStub(
                    TransactionLifecycleWorkflow::class.java,
                    workflowId,
                )
            workflow.onStatusUpdate(signal.toTemporalSignal())
        } catch (e: WorkflowNotFoundException) {
            log.warn("Workflow not found for signal: workflowId={}, externalTxId={}", workflowId, externalTxId, e)
        } catch (e: WorkflowServiceException) {
            log.error("Failed to signal workflow: workflowId={}, externalTxId={}", workflowId, externalTxId, e)
            throw e
        }
    }
}
