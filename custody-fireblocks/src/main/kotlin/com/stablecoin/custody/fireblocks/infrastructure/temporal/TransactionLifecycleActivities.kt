package com.stablecoin.custody.fireblocks.infrastructure.temporal

import com.stablecoin.custody.fireblocks.infrastructure.temporal.dto.CreateTransactionResult
import com.stablecoin.custody.fireblocks.infrastructure.temporal.dto.FireblocksSubmitActivityCommand
import com.stablecoin.custody.fireblocks.infrastructure.temporal.dto.PollStatusResult
import com.stablecoin.custody.fireblocks.infrastructure.temporal.dto.ReleaseFundsActivityCommand
import com.stablecoin.custody.fireblocks.infrastructure.temporal.dto.ReserveFundsActivityCommand
import com.stablecoin.custody.fireblocks.infrastructure.temporal.dto.ReserveFundsResult
import com.stablecoin.custody.fireblocks.infrastructure.temporal.dto.StartTransactionRequest
import com.stablecoin.custody.fireblocks.infrastructure.temporal.dto.SubmitResult
import io.temporal.activity.ActivityInterface

@ActivityInterface
interface TransactionLifecycleActivities {
    fun createTransaction(request: StartTransactionRequest): CreateTransactionResult

    fun recordSubmission(
        transactionId: String,
        fireblocksTransactionId: String,
    )

    fun recordStatusChange(
        transactionId: String,
        newStatus: String,
        fireblocksStatus: String,
        subStatus: String?,
        txHash: String?,
    )

    fun completeTransaction(
        transactionId: String,
        newStatus: String,
        fireblocksStatus: String,
        subStatus: String?,
        txHash: String?,
    )

    fun submitToFireblocks(command: FireblocksSubmitActivityCommand): SubmitResult

    fun fetchTransactionStatus(fireblocksTransactionId: String): PollStatusResult

    fun reserveFunds(command: ReserveFundsActivityCommand): ReserveFundsResult

    fun releaseFunds(command: ReleaseFundsActivityCommand)
}
