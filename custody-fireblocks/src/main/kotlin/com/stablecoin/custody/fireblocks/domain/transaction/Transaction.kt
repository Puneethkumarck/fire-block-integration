package com.stablecoin.custody.fireblocks.domain.transaction

import com.stablecoin.custody.fireblocks.domain.shared.StateProvider
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

data class Transaction(
    val id: TransactionId,
    val externalTxId: String,
    val fireblocksTransactionId: String?,
    val status: TransactionStatus,
    val fireblocksStatus: String?,
    val fireblocksSubStatus: String?,
    val assetId: String,
    val currency: String,
    val protocol: String,
    val amount: BigDecimal,
    val sourceVaultId: String,
    val destinationAddress: String,
    val feeLevel: FeeLevel,
    val treatAsGrossAmount: Boolean,
    val txHash: String?,
    val note: String?,
    val createdAt: Instant,
    val updatedAt: Instant,
    val version: Long = 0,
) : StateProvider<TransactionStatus> {
    init {
        require(externalTxId.isNotBlank()) { "externalTxId must not be blank" }
        require(amount > BigDecimal.ZERO) { "amount must be positive" }
        require(destinationAddress.isNotBlank()) { "destinationAddress must not be blank" }
    }

    override fun currentState() = status

    fun markSubmitted(fireblocksTransactionId: String): Transaction {
        require(fireblocksTransactionId.isNotBlank()) { "fireblocksTransactionId must not be blank" }
        return copy(
            fireblocksTransactionId = fireblocksTransactionId,
            status = TransactionStatus.SUBMITTED,
            updatedAt = Instant.now(),
        )
    }

    fun markFailed() =
        copy(
            status = TransactionStatus.FAILED,
            updatedAt = Instant.now(),
        )

    fun updateStatus(
        newStatus: TransactionStatus,
        fireblocksStatus: String,
        fireblocksSubStatus: String? = null,
        txHash: String? = null,
    ) = copy(
        status = newStatus,
        fireblocksStatus = fireblocksStatus,
        fireblocksSubStatus = fireblocksSubStatus,
        txHash = txHash ?: this.txHash,
        updatedAt = Instant.now(),
    )

    companion object {
        fun create(
            command: SubmitTransactionCommand,
            fireblocksAssetId: String,
        ) = Transaction(
            id = TransactionId(UUID.randomUUID()),
            externalTxId = command.externalTxId,
            fireblocksTransactionId = null,
            status = TransactionStatus.CREATED,
            fireblocksStatus = null,
            fireblocksSubStatus = null,
            assetId = fireblocksAssetId,
            currency = command.currency,
            protocol = command.protocol,
            amount = command.amount,
            sourceVaultId = command.sourceVaultId,
            destinationAddress = command.destinationAddress,
            feeLevel = command.feeLevel ?: FeeLevel.MEDIUM,
            treatAsGrossAmount = command.treatAsGrossAmount ?: false,
            txHash = null,
            note = command.note,
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
        )
    }
}
