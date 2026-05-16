package com.stablecoin.custody.fireblocks.api.response

import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

data class TransactionResponse(
    val id: UUID,
    val externalTxId: String,
    val fireblocksTransactionId: String?,
    val status: String,
    val currency: String,
    val protocol: String,
    val amount: BigDecimal,
    val sourceVaultId: String,
    val destinationAddress: String,
    val feeLevel: String,
    val note: String?,
    val txHash: String?,
    val createdAt: Instant,
    val updatedAt: Instant,
)
