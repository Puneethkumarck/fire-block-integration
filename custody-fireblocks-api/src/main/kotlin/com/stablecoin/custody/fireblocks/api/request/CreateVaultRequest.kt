package com.stablecoin.custody.fireblocks.api.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class CreateVaultRequest(
    @field:NotBlank @field:Size(max = 128) val customerRefId: String,
    @field:NotBlank @field:Size(max = 128) val name: String,
)
