package com.stablecoin.custody.fireblocks.test.fixtures

import com.stablecoin.custody.fireblocks.domain.vault.CreateVaultCommand
import com.stablecoin.custody.fireblocks.domain.vault.Vault
import com.stablecoin.custody.fireblocks.domain.vault.VaultId
import com.stablecoin.custody.fireblocks.domain.vault.VaultStatus
import java.time.Instant
import java.util.UUID

fun aVault(
    id: VaultId = VaultId(UUID.randomUUID()),
    fireblocksVaultId: String? = "fb-vault-123",
    customerRefId: String = "kyc-ref-${UUID.randomUUID()}",
    name: String = "Test Vault",
    status: VaultStatus = VaultStatus.ACTIVE,
    createdAt: Instant = Instant.now(),
    updatedAt: Instant = Instant.now(),
    version: Long = 0,
) = Vault(
    id = id,
    fireblocksVaultId = fireblocksVaultId,
    customerRefId = customerRefId,
    name = name,
    status = status,
    createdAt = createdAt,
    updatedAt = updatedAt,
    version = version,
)

fun aCreateVaultCommand(
    customerRefId: String = "customer-ref-001",
    name: String = "Test Vault",
) = CreateVaultCommand(
    customerRefId = customerRefId,
    name = name,
)
