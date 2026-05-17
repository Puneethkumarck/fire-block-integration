package com.stablecoin.custody.fireblocks.application.controller

import com.stablecoin.custody.fireblocks.api.request.CreateVaultRequest
import com.stablecoin.custody.fireblocks.api.response.VaultResponse
import com.stablecoin.custody.fireblocks.application.mapper.toCommand
import com.stablecoin.custody.fireblocks.application.mapper.toResponse
import com.stablecoin.custody.fireblocks.domain.vault.VaultCreationService
import com.stablecoin.custody.fireblocks.domain.vault.VaultId
import com.stablecoin.custody.fireblocks.domain.vault.VaultQueryService
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
class VaultController(
    private val vaultCreationService: VaultCreationService,
    private val vaultQueryService: VaultQueryService,
) {
    @PostMapping("/vaults")
    fun createVault(
        @RequestBody @Valid request: CreateVaultRequest,
    ): ResponseEntity<VaultResponse> {
        val vault = vaultCreationService.createVault(request.toCommand())
        return ResponseEntity.status(HttpStatus.CREATED).body(vault.toResponse())
    }

    @GetMapping("/vaults/{vaultId}")
    fun getVault(
        @PathVariable vaultId: UUID,
    ): ResponseEntity<VaultResponse> {
        val vault = vaultQueryService.getVault(VaultId(vaultId))
        return ResponseEntity.ok(vault.toResponse())
    }
}
