package com.stablecoin.custody.fireblocks.test.fixtures

import com.stablecoin.custody.fireblocks.domain.port.VaultResult

fun aVaultResult(
    id: String = "fireblocks-vault-456",
    name: String = "Test Vault",
) = VaultResult(id = id, name = name)
