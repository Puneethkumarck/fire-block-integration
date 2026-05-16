package com.stablecoin.custody.fireblocks.domain.vault

import java.time.Instant

interface VaultRepository {
    fun findById(id: VaultId): Vault?

    fun findByCustomerRefId(customerRefId: String): Vault?

    fun save(vault: Vault): Vault

    fun findPendingOlderThan(
        cutoff: Instant,
        limit: Int,
    ): List<Vault>
}
