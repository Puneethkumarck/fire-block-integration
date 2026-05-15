package com.stablecoin.custody.fireblocks.domain.transaction

import java.util.UUID

@JvmInline
value class TransactionId(
    val value: UUID,
)
