package com.stablecoin.custody.fireblocks.domain.port

interface FireblocksBalancePort {
    fun getBalance(
        vaultAccountId: String,
        assetId: String,
        refresh: Boolean,
    ): BalanceResult
}
