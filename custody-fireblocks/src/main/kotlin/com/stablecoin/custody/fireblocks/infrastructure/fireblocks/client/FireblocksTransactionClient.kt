package com.stablecoin.custody.fireblocks.infrastructure.fireblocks.client

import com.stablecoin.custody.fireblocks.infrastructure.fireblocks.client.dto.CreateTransactionRequest
import com.stablecoin.custody.fireblocks.infrastructure.fireblocks.client.dto.FireblocksCancelTransactionResponse
import com.stablecoin.custody.fireblocks.infrastructure.fireblocks.client.dto.FireblocksEstimateFeeRequest
import com.stablecoin.custody.fireblocks.infrastructure.fireblocks.client.dto.FireblocksEstimateFeeResponse
import com.stablecoin.custody.fireblocks.infrastructure.fireblocks.client.dto.FireblocksTransactionResponse
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.service.annotation.GetExchange
import org.springframework.web.service.annotation.HttpExchange
import org.springframework.web.service.annotation.PostExchange

@HttpExchange
interface FireblocksTransactionClient {
    @PostExchange("/v1/transactions")
    fun createTransaction(
        @RequestBody request: CreateTransactionRequest,
    ): FireblocksTransactionResponse

    @GetExchange("/v1/transactions/{txId}")
    fun getTransaction(
        @PathVariable txId: String,
    ): FireblocksTransactionResponse

    @GetExchange("/v1/transactions/external_tx_id/{externalTxId}")
    fun getByExternalId(
        @PathVariable externalTxId: String,
    ): FireblocksTransactionResponse

    @PostExchange("/v1/transactions/estimate_fee")
    fun estimateFee(
        @RequestBody request: FireblocksEstimateFeeRequest,
    ): FireblocksEstimateFeeResponse

    @PostExchange("/v1/transactions/{txId}/cancel")
    fun cancelTransaction(
        @PathVariable txId: String,
    ): FireblocksCancelTransactionResponse
}
