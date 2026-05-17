package com.stablecoin.custody.fireblocks.application.controller

import com.stablecoin.custody.fireblocks.api.response.BalanceResponse
import com.stablecoin.custody.fireblocks.domain.port.BalanceResult
import com.stablecoin.custody.fireblocks.domain.vault.VaultId
import com.stablecoin.custody.fireblocks.domain.wallet.BalanceQueryService
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@Validated
@RestController
@RequestMapping("/api/v1")
class BalanceController(
    private val balanceQueryService: BalanceQueryService,
) {
    @GetMapping("/vaults/{vaultId}/assets/{currency}/{protocol}/balance")
    fun getBalance(
        @PathVariable vaultId: UUID,
        @PathVariable currency: String,
        @PathVariable protocol: String,
        @RequestParam(defaultValue = "false") refresh: Boolean,
    ): ResponseEntity<BalanceResponse> {
        val balance = balanceQueryService.getBalance(VaultId(vaultId), currency, protocol, refresh)
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
