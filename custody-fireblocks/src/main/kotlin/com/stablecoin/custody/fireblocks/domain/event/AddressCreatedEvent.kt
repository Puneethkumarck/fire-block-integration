package com.stablecoin.custody.fireblocks.domain.event

import com.stablecoin.custody.fireblocks.domain.vault.VaultId
import com.stablecoin.custody.fireblocks.domain.wallet.DepositAddress
import java.time.Instant
import java.util.UUID

data class AddressCreatedEvent(
    val depositAddressId: UUID,
    val walletAssetId: UUID,
    val vaultId: UUID,
    val address: String,
    val tag: String?,
    val occurredAt: Instant,
) {
    companion object {
        const val TOPIC = "custody.deposit-address.generated"

        fun from(
            depositAddress: DepositAddress,
            vaultId: VaultId,
        ) = AddressCreatedEvent(
            depositAddressId = depositAddress.id.value,
            walletAssetId = depositAddress.walletAssetId.value,
            vaultId = vaultId.value,
            address = depositAddress.address,
            tag = depositAddress.tag,
            occurredAt = Instant.now(),
        )
    }
}
