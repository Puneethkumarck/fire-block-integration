package com.stablecoin.custody.fireblocks.infrastructure.fireblocks.adapter

import com.stablecoin.custody.fireblocks.domain.port.BalanceResult
import com.stablecoin.custody.fireblocks.domain.port.FireblocksBalancePort
import com.stablecoin.custody.fireblocks.domain.shared.logger
import com.stablecoin.custody.fireblocks.infrastructure.fireblocks.client.FireblocksVaultClient
import com.stablecoin.custody.fireblocks.infrastructure.fireblocks.client.dto.FireblocksBalanceResponse
import io.github.resilience4j.bulkhead.annotation.Bulkhead
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker
import io.github.resilience4j.retry.annotation.Retry
import org.springframework.stereotype.Component
import java.math.BigDecimal

@Component
internal class FireblocksBalanceAdapter(
    private val vaultClient: FireblocksVaultClient,
) : FireblocksBalancePort {
    private val log = logger<FireblocksBalanceAdapter>()

    @Bulkhead(name = "fireblocks")
    @CircuitBreaker(name = "fireblocks-balance")
    @Retry(name = "fireblocks")
    override fun getBalance(
        vaultAccountId: String,
        assetId: String,
        refresh: Boolean,
    ): BalanceResult {
        log.info(
            "Getting balance: vaultAccountId={}, assetId={}, refresh={}",
            vaultAccountId,
            assetId,
            refresh,
        )
        val response = vaultClient.refreshBalance(vaultAccountId, assetId)
        return response.toBalanceResult()
    }

    fun FireblocksBalanceResponse.toBalanceResult() =
        BalanceResult(
            total = BigDecimal(total),
            available = BigDecimal(available),
            pending = BigDecimal(pending),
            frozen = BigDecimal(frozen),
            locked = BigDecimal(locked),
        )
}
