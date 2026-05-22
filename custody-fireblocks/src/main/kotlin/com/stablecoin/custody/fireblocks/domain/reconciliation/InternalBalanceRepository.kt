package com.stablecoin.custody.fireblocks.domain.reconciliation

import java.util.UUID

interface InternalBalanceRepository {
    fun findByVaultIdAndCurrencyAndProtocol(
        vaultId: UUID,
        currency: String,
        protocol: String,
    ): InternalBalance?

    fun save(balance: InternalBalance): InternalBalance
}
