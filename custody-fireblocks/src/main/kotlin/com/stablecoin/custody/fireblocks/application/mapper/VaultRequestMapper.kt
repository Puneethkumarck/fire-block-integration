package com.stablecoin.custody.fireblocks.application.mapper

import com.stablecoin.custody.fireblocks.api.request.CreateVaultRequest
import com.stablecoin.custody.fireblocks.domain.vault.CreateVaultCommand

fun CreateVaultRequest.toCommand() =
    CreateVaultCommand(
        customerRefId = customerRefId,
        name = name,
    )
