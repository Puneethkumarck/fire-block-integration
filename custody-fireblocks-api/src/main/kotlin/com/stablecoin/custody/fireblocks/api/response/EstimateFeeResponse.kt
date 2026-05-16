package com.stablecoin.custody.fireblocks.api.response

import java.math.BigDecimal

data class EstimateFeeResponse(
    val low: BigDecimal,
    val medium: BigDecimal,
    val high: BigDecimal,
)
