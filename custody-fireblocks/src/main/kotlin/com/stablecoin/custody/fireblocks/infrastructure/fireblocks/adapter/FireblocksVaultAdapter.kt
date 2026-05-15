package com.stablecoin.custody.fireblocks.infrastructure.fireblocks.adapter

import com.stablecoin.custody.fireblocks.domain.port.DepositAddressResult
import com.stablecoin.custody.fireblocks.domain.port.FireblocksVaultPort
import com.stablecoin.custody.fireblocks.domain.port.VaultResult
import com.stablecoin.custody.fireblocks.domain.port.WalletAssetResult
import com.stablecoin.custody.fireblocks.domain.shared.logger
import com.stablecoin.custody.fireblocks.infrastructure.fireblocks.client.FireblocksVaultClient
import com.stablecoin.custody.fireblocks.infrastructure.fireblocks.client.dto.CreateVaultAccountRequest
import com.stablecoin.custody.fireblocks.infrastructure.fireblocks.client.dto.FireblocksDepositAddressResponse
import com.stablecoin.custody.fireblocks.infrastructure.fireblocks.client.dto.FireblocksVaultAccountResponse
import com.stablecoin.custody.fireblocks.infrastructure.fireblocks.client.dto.FireblocksWalletAssetResponse
import org.springframework.stereotype.Component

@Component
internal class FireblocksVaultAdapter(
    private val vaultClient: FireblocksVaultClient,
) : FireblocksVaultPort {
    private val log = logger<FireblocksVaultAdapter>()

    override fun createVault(
        name: String,
        customerRefId: String,
    ): VaultResult {
        log.info("Creating Fireblocks vault: name={}, customerRefId={}", name, customerRefId)
        val response = vaultClient.createVault(CreateVaultAccountRequest(name = name, customerRefId = customerRefId))
        return response.toVaultResult()
    }

    override fun getVault(vaultAccountId: String): VaultResult {
        log.info("Getting Fireblocks vault: vaultAccountId={}", vaultAccountId)
        val response = vaultClient.getVault(vaultAccountId)
        return response.toVaultResult()
    }

    override fun createWalletAsset(
        vaultAccountId: String,
        assetId: String,
    ): WalletAssetResult {
        log.info("Creating wallet asset: vaultAccountId={}, assetId={}", vaultAccountId, assetId)
        val response = vaultClient.createWalletAsset(vaultAccountId, assetId)
        return response.toWalletAssetResult()
    }

    override fun generateDepositAddress(
        vaultAccountId: String,
        assetId: String,
    ): DepositAddressResult {
        log.info("Generating deposit address: vaultAccountId={}, assetId={}", vaultAccountId, assetId)
        val response = vaultClient.generateDepositAddress(vaultAccountId, assetId, null)
        return response.toDepositAddressResult()
    }

    fun FireblocksVaultAccountResponse.toVaultResult() =
        VaultResult(
            id = id,
            name = name,
        )

    fun FireblocksWalletAssetResponse.toWalletAssetResult() =
        WalletAssetResult(
            id = id,
            available = available,
        )

    fun FireblocksDepositAddressResponse.toDepositAddressResult() =
        DepositAddressResult(
            address = address,
            tag = tag,
            legacyAddress = legacyAddress,
        )
}
