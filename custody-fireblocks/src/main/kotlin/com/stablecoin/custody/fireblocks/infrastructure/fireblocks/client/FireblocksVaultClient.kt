package com.stablecoin.custody.fireblocks.infrastructure.fireblocks.client

import com.stablecoin.custody.fireblocks.infrastructure.fireblocks.client.dto.CreateVaultAccountRequest
import com.stablecoin.custody.fireblocks.infrastructure.fireblocks.client.dto.FireblocksBalanceResponse
import com.stablecoin.custody.fireblocks.infrastructure.fireblocks.client.dto.FireblocksDepositAddressResponse
import com.stablecoin.custody.fireblocks.infrastructure.fireblocks.client.dto.FireblocksVaultAccountResponse
import com.stablecoin.custody.fireblocks.infrastructure.fireblocks.client.dto.FireblocksWalletAssetResponse
import com.stablecoin.custody.fireblocks.infrastructure.fireblocks.client.dto.GenerateAddressRequest
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.service.annotation.GetExchange
import org.springframework.web.service.annotation.HttpExchange
import org.springframework.web.service.annotation.PostExchange

@HttpExchange
interface FireblocksVaultClient {
    @PostExchange("/v1/vault/accounts")
    fun createVault(
        @RequestBody request: CreateVaultAccountRequest,
    ): FireblocksVaultAccountResponse

    @GetExchange("/v1/vault/accounts/{vaultAccountId}")
    fun getVault(
        @PathVariable vaultAccountId: String,
    ): FireblocksVaultAccountResponse

    @PostExchange("/v1/vault/accounts/{vaultAccountId}/{assetId}")
    fun createWalletAsset(
        @PathVariable vaultAccountId: String,
        @PathVariable assetId: String,
    ): FireblocksWalletAssetResponse

    @PostExchange("/v1/vault/accounts/{vaultAccountId}/{assetId}/addresses")
    fun generateDepositAddress(
        @PathVariable vaultAccountId: String,
        @PathVariable assetId: String,
        @RequestBody request: GenerateAddressRequest?,
    ): FireblocksDepositAddressResponse

    @PostExchange("/v1/vault/accounts/{vaultAccountId}/{assetId}/balance")
    fun refreshBalance(
        @PathVariable vaultAccountId: String,
        @PathVariable assetId: String,
    ): FireblocksBalanceResponse
}
