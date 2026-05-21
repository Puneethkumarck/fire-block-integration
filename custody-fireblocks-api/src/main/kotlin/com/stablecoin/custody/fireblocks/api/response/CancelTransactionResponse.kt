package com.stablecoin.custody.fireblocks.api.response

import java.util.UUID

data class CancelTransactionResponse(
    val transactionId: UUID,
    val status: String,
    val message: String,
)
