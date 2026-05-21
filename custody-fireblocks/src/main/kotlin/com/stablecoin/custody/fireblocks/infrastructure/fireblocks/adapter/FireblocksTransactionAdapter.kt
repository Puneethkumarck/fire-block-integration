package com.stablecoin.custody.fireblocks.infrastructure.fireblocks.adapter

import com.stablecoin.custody.fireblocks.domain.port.FeeEstimateResult
import com.stablecoin.custody.fireblocks.domain.port.FireblocksEstimateFeeCommand
import com.stablecoin.custody.fireblocks.domain.port.FireblocksSubmitCommand
import com.stablecoin.custody.fireblocks.domain.port.FireblocksTransactionPort
import com.stablecoin.custody.fireblocks.domain.port.TransactionResult
import com.stablecoin.custody.fireblocks.domain.shared.logger
import com.stablecoin.custody.fireblocks.infrastructure.fireblocks.client.FireblocksTransactionClient
import com.stablecoin.custody.fireblocks.infrastructure.fireblocks.client.dto.CreateTransactionRequest
import com.stablecoin.custody.fireblocks.infrastructure.fireblocks.client.dto.DestinationTransferPeerPath
import com.stablecoin.custody.fireblocks.infrastructure.fireblocks.client.dto.FireblocksEstimateFeeRequest
import com.stablecoin.custody.fireblocks.infrastructure.fireblocks.client.dto.FireblocksEstimateFeeResponse
import com.stablecoin.custody.fireblocks.infrastructure.fireblocks.client.dto.FireblocksTransactionResponse
import com.stablecoin.custody.fireblocks.infrastructure.fireblocks.client.dto.OneTimeAddress
import com.stablecoin.custody.fireblocks.infrastructure.fireblocks.client.dto.TransferPeerPath
import io.github.resilience4j.bulkhead.annotation.Bulkhead
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker
import io.github.resilience4j.retry.annotation.Retry
import org.springframework.stereotype.Component
import java.math.BigDecimal

@Component
internal class FireblocksTransactionAdapter(
    private val transactionClient: FireblocksTransactionClient,
) : FireblocksTransactionPort {
    private val log = logger<FireblocksTransactionAdapter>()

    @Bulkhead(name = "fireblocks")
    @CircuitBreaker(name = "fireblocks")
    override fun submitTransaction(command: FireblocksSubmitCommand): TransactionResult {
        log.info(
            "Submitting transaction: externalTxId={}, sourceVaultId={}, assetId={}",
            command.externalTxId,
            command.sourceVaultId,
            command.assetId,
        )
        val request = command.toCreateTransactionRequest()
        val response = transactionClient.createTransaction(request)
        return response.toTransactionResult()
    }

    @Bulkhead(name = "fireblocks")
    @CircuitBreaker(name = "fireblocks")
    @Retry(name = "fireblocks")
    override fun getTransaction(fireblocksTxId: String): TransactionResult {
        log.info("Getting transaction: fireblocksTxId={}", fireblocksTxId)
        val response = transactionClient.getTransaction(fireblocksTxId)
        return response.toTransactionResult()
    }

    @Bulkhead(name = "fireblocks")
    @CircuitBreaker(name = "fireblocks")
    @Retry(name = "fireblocks")
    override fun getByExternalId(externalTxId: String): TransactionResult? {
        log.info("Getting transaction by externalId: externalTxId={}", externalTxId)
        return try {
            val response = transactionClient.getByExternalId(externalTxId)
            response.toTransactionResult()
        } catch (_: org.springframework.web.client.HttpClientErrorException.NotFound) {
            null
        }
    }

    @Bulkhead(name = "fireblocks")
    @CircuitBreaker(name = "fireblocks")
    @Retry(name = "fireblocks")
    override fun estimateFee(command: FireblocksEstimateFeeCommand): FeeEstimateResult {
        log.info(
            "Estimating fee: sourceVaultId={}, assetId={}",
            command.sourceVaultId,
            command.assetId,
        )
        val request = command.toEstimateFeeRequest()
        val response = transactionClient.estimateFee(request)
        return response.toFeeEstimateResult()
    }

    @Bulkhead(name = "fireblocks")
    @CircuitBreaker(name = "fireblocks")
    override fun cancelTransaction(fireblocksTxId: String): Boolean {
        log.info("Cancelling transaction: fireblocksTxId={}", fireblocksTxId)
        return try {
            val response = transactionClient.cancelTransaction(fireblocksTxId)
            response.success
        } catch (_: org.springframework.web.client.HttpClientErrorException.BadRequest) {
            false
        }
    }

    fun FireblocksSubmitCommand.toCreateTransactionRequest() =
        CreateTransactionRequest(
            externalTxId = externalTxId,
            source = TransferPeerPath(type = "VAULT_ACCOUNT", id = sourceVaultId),
            destination =
                DestinationTransferPeerPath(
                    type = "ONE_TIME_ADDRESS",
                    oneTimeAddress = OneTimeAddress(address = destinationAddress),
                ),
            assetId = assetId,
            amount = amount.toPlainString(),
            feeLevel = feeLevel.name,
            treatAsGrossAmount = treatAsGrossAmount,
            note = note,
        )

    fun FireblocksEstimateFeeCommand.toEstimateFeeRequest() =
        FireblocksEstimateFeeRequest(
            assetId = assetId,
            source = TransferPeerPath(type = "VAULT_ACCOUNT", id = sourceVaultId),
            destination =
                DestinationTransferPeerPath(
                    type = "ONE_TIME_ADDRESS",
                    oneTimeAddress = OneTimeAddress(address = destinationAddress),
                ),
            amount = amount.toPlainString(),
        )

    fun FireblocksTransactionResponse.toTransactionResult() =
        TransactionResult(
            id = id,
            status = status,
            subStatus = subStatus,
            txHash = txHash,
        )

    fun FireblocksEstimateFeeResponse.toFeeEstimateResult() =
        FeeEstimateResult(
            low = low.networkFee?.let { BigDecimal(it) } ?: BigDecimal.ZERO,
            medium = medium.networkFee?.let { BigDecimal(it) } ?: BigDecimal.ZERO,
            high = high.networkFee?.let { BigDecimal(it) } ?: BigDecimal.ZERO,
        )
}
