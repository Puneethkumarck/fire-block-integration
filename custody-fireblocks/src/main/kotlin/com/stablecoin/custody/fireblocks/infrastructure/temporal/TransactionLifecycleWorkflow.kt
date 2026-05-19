package com.stablecoin.custody.fireblocks.infrastructure.temporal

import com.stablecoin.custody.fireblocks.infrastructure.temporal.dto.StartTransactionRequest
import com.stablecoin.custody.fireblocks.infrastructure.temporal.dto.TemporalTransactionStatusSignal
import io.temporal.workflow.SignalMethod
import io.temporal.workflow.WorkflowInterface
import io.temporal.workflow.WorkflowMethod

@WorkflowInterface
interface TransactionLifecycleWorkflow {
    @WorkflowMethod
    fun execute(request: StartTransactionRequest)

    @SignalMethod
    fun onStatusUpdate(signal: TemporalTransactionStatusSignal)
}
