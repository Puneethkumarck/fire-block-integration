package com.stablecoin.custody.fireblocks.api.response

import java.math.BigDecimal

data class BalanceResponse(
    val total: BigDecimal,
    val available: BigDecimal,
    val pending: BigDecimal,
    val frozen: BigDecimal,
    val locked: BigDecimal,
)
