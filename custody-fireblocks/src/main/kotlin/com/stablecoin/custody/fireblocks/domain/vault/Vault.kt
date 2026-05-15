package com.stablecoin.custody.fireblocks.domain.vault

import com.stablecoin.custody.fireblocks.domain.exception.VaultNotActiveException
import java.time.Instant
import java.util.UUID

data class Vault(
    val id: VaultId,
    val fireblocksVaultId: String?,
    val customerRefId: String,
    val name: String,
    val status: VaultStatus,
    val createdAt: Instant,
    val updatedAt: Instant,
    val version: Long = 0,
) {
    init {
        require(customerRefId.isNotBlank()) { "customerRefId must not be blank" }
        require(name.isNotBlank()) { "name must not be blank" }
    }

    fun activate(fireblocksVaultId: String) =
        copy(
            fireblocksVaultId = fireblocksVaultId,
            status = VaultStatus.ACTIVE,
            updatedAt = Instant.now(),
        )

    fun markFailed() =
        copy(
            status = VaultStatus.FAILED,
            updatedAt = Instant.now(),
        )

    fun assertActive() {
        if (status != VaultStatus.ACTIVE) {
            throw VaultNotActiveException(id.value.toString())
        }
    }

    companion object {
        fun create(command: CreateVaultCommand) =
            Vault(
                id = VaultId(UUID.randomUUID()),
                fireblocksVaultId = null,
                customerRefId = command.customerRefId,
                name = command.name,
                status = VaultStatus.PENDING,
                createdAt = Instant.now(),
                updatedAt = Instant.now(),
            )
    }
}
