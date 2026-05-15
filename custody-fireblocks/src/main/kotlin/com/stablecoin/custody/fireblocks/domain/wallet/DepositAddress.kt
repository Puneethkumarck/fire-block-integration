package com.stablecoin.custody.fireblocks.domain.wallet

import java.time.Instant
import java.util.UUID

data class DepositAddress(
    val id: DepositAddressId,
    val walletAssetId: WalletAssetId,
    val address: String,
    val tag: String?,
    val legacyAddress: String?,
    val createdAt: Instant,
    val updatedAt: Instant,
    val version: Long = 0,
) {
    init {
        require(address.isNotBlank()) { "address must not be blank" }
    }

    companion object {
        fun create(
            walletAssetId: WalletAssetId,
            address: String,
            tag: String? = null,
            legacyAddress: String? = null,
        ): DepositAddress {
            val now = Instant.now()
            return DepositAddress(
                id = DepositAddressId(UUID.randomUUID()),
                walletAssetId = walletAssetId,
                address = address,
                tag = tag,
                legacyAddress = legacyAddress,
                createdAt = now,
                updatedAt = now,
            )
        }
    }
}
