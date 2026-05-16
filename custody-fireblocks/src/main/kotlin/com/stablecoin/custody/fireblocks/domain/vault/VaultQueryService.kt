package com.stablecoin.custody.fireblocks.domain.vault

import com.stablecoin.custody.fireblocks.domain.exception.VaultNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class VaultQueryService(
    private val vaultRepository: VaultRepository,
) {
    @Transactional(readOnly = true)
    fun getVault(id: VaultId): Vault = vaultRepository.findById(id) ?: throw VaultNotFoundException(id.value.toString())

    @Transactional(readOnly = true)
    fun getVaultByCustomerRefId(customerRefId: String): Vault =
        vaultRepository.findByCustomerRefId(customerRefId)
            ?: throw VaultNotFoundException(customerRefId)
}
