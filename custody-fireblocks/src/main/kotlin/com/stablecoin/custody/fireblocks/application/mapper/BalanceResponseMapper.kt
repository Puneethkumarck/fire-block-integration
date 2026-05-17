package com.stablecoin.custody.fireblocks.application.mapper

import com.stablecoin.custody.fireblocks.api.response.BalanceResponse
import com.stablecoin.custody.fireblocks.domain.port.BalanceResult

fun BalanceResult.toResponse() =
    BalanceResponse(
        total = total,
        available = available,
        pending = pending,
        frozen = frozen,
        locked = locked,
    )
