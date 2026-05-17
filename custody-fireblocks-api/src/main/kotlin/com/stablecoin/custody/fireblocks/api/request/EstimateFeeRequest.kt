package com.stablecoin.custody.fireblocks.api.request

import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import java.math.BigDecimal

private const val UUID_PATTERN = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"

data class EstimateFeeRequest(
    @field:NotBlank @field:Pattern(regexp = UUID_PATTERN) val sourceVaultId: String,
    @field:NotBlank @field:Size(max = 256) val destinationAddress: String,
    @field:NotBlank @field:Size(max = 10) val currency: String,
    @field:NotBlank @field:Size(max = 20) val protocol: String,
    @field:NotNull @field:DecimalMin("0.000000000000000001") val amount: BigDecimal,
)
