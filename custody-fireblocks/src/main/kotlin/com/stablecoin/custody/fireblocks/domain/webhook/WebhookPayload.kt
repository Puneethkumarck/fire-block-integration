package com.stablecoin.custody.fireblocks.domain.webhook

data class WebhookPayload(
    val type: String,
    val tenantId: String?,
    val timestamp: Long?,
    val createdAt: Long?,
    val data: WebhookTransactionData?,
)

data class WebhookTransactionData(
    val id: String? = null,
    val status: String? = null,
    val subStatus: String? = null,
    val txHash: String? = null,
)
