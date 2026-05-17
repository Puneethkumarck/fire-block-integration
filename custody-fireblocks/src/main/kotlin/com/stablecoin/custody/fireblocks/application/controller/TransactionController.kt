package com.stablecoin.custody.fireblocks.application.controller

import com.stablecoin.custody.fireblocks.api.request.EstimateFeeRequest
import com.stablecoin.custody.fireblocks.api.request.SubmitTransactionRequest
import com.stablecoin.custody.fireblocks.api.response.EstimateFeeResponse
import com.stablecoin.custody.fireblocks.api.response.TransactionResponse
import com.stablecoin.custody.fireblocks.application.mapper.toCommand
import com.stablecoin.custody.fireblocks.application.mapper.toEstimateFeeCommand
import com.stablecoin.custody.fireblocks.application.mapper.toResponse
import com.stablecoin.custody.fireblocks.domain.exception.AssetNotFoundException
import com.stablecoin.custody.fireblocks.domain.port.FireblocksTransactionPort
import com.stablecoin.custody.fireblocks.domain.transaction.TransactionQueryService
import com.stablecoin.custody.fireblocks.domain.transaction.TransactionSubmissionHandler
import com.stablecoin.custody.fireblocks.domain.vault.VaultId
import com.stablecoin.custody.fireblocks.domain.vault.VaultQueryService
import com.stablecoin.custody.fireblocks.domain.wallet.SupportedAssetRepository
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@Validated
@RestController
@RequestMapping("/api/v1")
class TransactionController(
    private val transactionSubmissionHandler: TransactionSubmissionHandler,
    private val transactionQueryService: TransactionQueryService,
    private val vaultQueryService: VaultQueryService,
    private val supportedAssetRepository: SupportedAssetRepository,
    private val fireblocksTransactionPort: FireblocksTransactionPort,
) {
    @PostMapping("/transactions")
    fun submitTransaction(
        @RequestBody @Valid request: SubmitTransactionRequest,
    ): ResponseEntity<TransactionResponse> {
        val command = request.toCommand()
        val transaction = transactionSubmissionHandler.handle(command)
        return ResponseEntity.status(HttpStatus.CREATED).body(transaction.toResponse())
    }

    @GetMapping("/transactions/{externalTxId}")
    fun getTransaction(
        @PathVariable externalTxId: String,
    ): ResponseEntity<TransactionResponse> {
        val transaction = transactionQueryService.getByExternalTxId(externalTxId)
        return ResponseEntity.ok(transaction.toResponse())
    }

    @GetMapping("/transactions/fireblocks/{fireblocksTxId}")
    fun getByFireblocksTxId(
        @PathVariable fireblocksTxId: String,
    ): ResponseEntity<TransactionResponse> {
        val transaction = transactionQueryService.getByFireblocksTxId(fireblocksTxId)
        return ResponseEntity.ok(transaction.toResponse())
    }

    @PostMapping("/transactions/estimate-fee")
    fun estimateFee(
        @RequestBody @Valid request: EstimateFeeRequest,
    ): ResponseEntity<EstimateFeeResponse> {
        val vault = vaultQueryService.getVault(VaultId(UUID.fromString(request.sourceVaultId)))
        vault.assertActive()
        val fireblocksVaultId =
            vault.fireblocksVaultId
                ?: throw IllegalStateException("Active vault ${vault.id.value} missing fireblocksVaultId")
        val supportedAsset =
            supportedAssetRepository.findByCurrencyAndProtocol(request.currency, request.protocol)
                ?: throw AssetNotFoundException(request.sourceVaultId, "${request.currency}/${request.protocol}")
        val result =
            fireblocksTransactionPort.estimateFee(
                request.toEstimateFeeCommand(fireblocksVaultId, supportedAsset.fireblocksAssetId),
            )
        return ResponseEntity.ok(result.toResponse())
    }
}
