package com.stablecoin.custody.fireblocks.domain.wallet

import com.stablecoin.custody.fireblocks.domain.exception.AssetNotFoundException
import com.stablecoin.custody.fireblocks.domain.exception.VaultNotActiveException
import com.stablecoin.custody.fireblocks.domain.port.BalanceResult
import com.stablecoin.custody.fireblocks.domain.port.FireblocksBalancePort
import com.stablecoin.custody.fireblocks.domain.vault.VaultId
import com.stablecoin.custody.fireblocks.domain.vault.VaultQueryService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class BalanceQueryService(
    private val vaultQueryService: VaultQueryService,
    private val supportedAssetRepository: SupportedAssetRepository,
    private val fireblocksBalancePort: FireblocksBalancePort,
) {
    @Transactional(readOnly = true)
    fun getBalance(
        vaultId: VaultId,
        currency: String,
        protocol: String,
        refresh: Boolean,
    ): BalanceResult {
        val vault = vaultQueryService.getVault(vaultId)
        vault.assertActive()
        val supportedAsset =
            supportedAssetRepository.findByCurrencyAndProtocol(currency, protocol)
                ?: throw AssetNotFoundException(vaultId.value.toString(), "$currency/$protocol")
        val fireblocksVaultId =
            vault.fireblocksVaultId ?: throw VaultNotActiveException(vaultId.value.toString())
        return fireblocksBalancePort.getBalance(fireblocksVaultId, supportedAsset.fireblocksAssetId, refresh)
    }
}
