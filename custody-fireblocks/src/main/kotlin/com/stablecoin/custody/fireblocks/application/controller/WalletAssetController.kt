package com.stablecoin.custody.fireblocks.application.controller

import com.stablecoin.custody.fireblocks.api.request.ActivateAssetRequest
import com.stablecoin.custody.fireblocks.api.response.DepositAddressResponse
import com.stablecoin.custody.fireblocks.api.response.WalletAssetResponse
import com.stablecoin.custody.fireblocks.application.mapper.toResponse
import com.stablecoin.custody.fireblocks.domain.vault.VaultId
import com.stablecoin.custody.fireblocks.domain.wallet.ActivateAssetCommand
import com.stablecoin.custody.fireblocks.domain.wallet.GenerateAddressCommand
import com.stablecoin.custody.fireblocks.domain.wallet.WalletAssetService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@Validated
@RestController
@RequestMapping("/api/v1")
class WalletAssetController(
    private val walletAssetService: WalletAssetService,
) {
    @PostMapping("/vaults/{vaultId}/assets")
    fun activateAsset(
        @PathVariable vaultId: UUID,
        @RequestBody @Valid request: ActivateAssetRequest,
    ): ResponseEntity<WalletAssetResponse> {
        val command = ActivateAssetCommand(VaultId(vaultId), request.currency, request.protocol)
        val asset = walletAssetService.activateAsset(command)
        return ResponseEntity.status(HttpStatus.CREATED).body(asset.toResponse())
    }

    @PostMapping("/vaults/{vaultId}/assets/{currency}/{protocol}/addresses")
    fun generateAddress(
        @PathVariable vaultId: UUID,
        @PathVariable currency: String,
        @PathVariable protocol: String,
    ): ResponseEntity<DepositAddressResponse> {
        val command = GenerateAddressCommand(VaultId(vaultId), currency, protocol)
        val address = walletAssetService.generateDepositAddress(command)
        return ResponseEntity.status(HttpStatus.CREATED).body(address.toResponse())
    }
}
