package com.stablecoin.custody.fireblocks.domain.event

import com.stablecoin.custody.fireblocks.domain.vault.Vault
import java.time.Instant
import java.util.UUID

data class VaultCreatedEvent(
    val vaultId: UUID,
    val customerRefId: String,
    val fireblocksVaultId: String,
    val occurredAt: Instant,
) {
    companion object {
        const val TOPIC = "custody.vault.created"

        fun from(vault: Vault): VaultCreatedEvent {
            val fireblocksVaultId =
                requireNotNull(vault.fireblocksVaultId) {
                    "Cannot create VaultCreatedEvent: fireblocksVaultId is null for vault ${vault.id.value}"
                }
            return VaultCreatedEvent(
                vaultId = vault.id.value,
                customerRefId = vault.customerRefId,
                fireblocksVaultId = fireblocksVaultId,
                occurredAt = Instant.now(),
            )
        }
    }
}
