package com.stablecoin.custody.fireblocks.application.controller

import com.stablecoin.custody.fireblocks.api.response.BalanceResponse
import com.stablecoin.custody.fireblocks.domain.exception.AssetNotFoundException
import com.stablecoin.custody.fireblocks.domain.port.BalanceResult
import com.stablecoin.custody.fireblocks.domain.port.FireblocksBalancePort
import com.stablecoin.custody.fireblocks.domain.vault.VaultId
import com.stablecoin.custody.fireblocks.domain.vault.VaultQueryService
import com.stablecoin.custody.fireblocks.domain.wallet.SupportedAssetRepository
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1")
class BalanceController(
    private val vaultQueryService: VaultQueryService,
    private val supportedAssetRepository: SupportedAssetRepository,
    private val fireblocksBalancePort: FireblocksBalancePort,
) {
    @GetMapping("/vaults/{vaultId}/assets/{currency}/{protocol}/balance")
    fun getBalance(
        @PathVariable vaultId: UUID,
        @PathVariable currency: String,
        @PathVariable protocol: String,
        @RequestParam(defaultValue = "false") refresh: Boolean,
    ): ResponseEntity<BalanceResponse> {
        val vault = vaultQueryService.getVault(VaultId(vaultId))
        vault.assertActive()
        val supportedAsset =
            supportedAssetRepository.findByCurrencyAndProtocol(currency, protocol)
                ?: throw AssetNotFoundException(vaultId.toString(), "$currency/$protocol")
        val balance = fireblocksBalancePort.getBalance(vault.fireblocksVaultId!!, supportedAsset.fireblocksAssetId, refresh)
        return ResponseEntity.ok(balance.toResponse())
    }

    fun BalanceResult.toResponse() =
        BalanceResponse(
            total = total,
            available = available,
            pending = pending,
            frozen = frozen,
            locked = locked,
        )
}
