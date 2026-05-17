package com.stablecoin.custody.fireblocks.application.webhook

data class WebhookPayload(
    val type: String,
    val tenantId: String?,
    val timestamp: Long?,
    val createdAt: Long?,
    val data: WebhookTransactionData?,
)

data class WebhookTransactionData(
    val id: String,
    val status: String,
    val subStatus: String?,
    val txHash: String?,
)
