package com.stablecoin.custody.fireblocks.api.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class ActivateAssetRequest(
    @field:NotBlank @field:Size(max = 10) val currency: String,
    @field:NotBlank @field:Size(max = 20) val protocol: String,
)
