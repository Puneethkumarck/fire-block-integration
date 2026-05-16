package com.stablecoin.custody.fireblocks.api.error

data class ApiError(
    val code: String,
    val status: String,
    val message: String,
    val traceId: String? = null,
    val details: Map<String, String?>? = null,
)
