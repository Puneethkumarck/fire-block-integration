package com.stablecoin.custody.fireblocks.domain.wallet

import com.stablecoin.custody.fireblocks.domain.audit.AuditLog
import com.stablecoin.custody.fireblocks.domain.audit.AuditLogRepository
import com.stablecoin.custody.fireblocks.domain.audit.AuditOperation
import com.stablecoin.custody.fireblocks.domain.audit.AuditStatus
import com.stablecoin.custody.fireblocks.domain.event.AddressCreatedEvent
import com.stablecoin.custody.fireblocks.domain.event.WalletAssetCreatedEvent
import com.stablecoin.custody.fireblocks.domain.exception.AssetNotFoundException
import com.stablecoin.custody.fireblocks.domain.exception.VaultNotFoundException
import com.stablecoin.custody.fireblocks.domain.port.EventPublisher
import com.stablecoin.custody.fireblocks.domain.port.FireblocksVaultPort
import com.stablecoin.custody.fireblocks.domain.shared.logger
import com.stablecoin.custody.fireblocks.domain.vault.VaultRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

private val log = logger<WalletAssetService>()

@Service
class WalletAssetService(
    private val walletAssetRepository: WalletAssetRepository,
    private val depositAddressRepository: DepositAddressRepository,
    private val vaultRepository: VaultRepository,
    private val supportedAssetRepository: SupportedAssetRepository,
    private val fireblocksClient: FireblocksVaultPort,
    private val assetEventPublisher: EventPublisher<WalletAssetCreatedEvent>,
    private val addressEventPublisher: EventPublisher<AddressCreatedEvent>,
    private val auditLogRepository: AuditLogRepository,
) {
    @Transactional
    fun activateAsset(command: ActivateAssetCommand): WalletAsset {
        val vault =
            vaultRepository.findById(command.vaultId)
                ?: throw VaultNotFoundException(command.vaultId.value.toString())
        vault.assertActive()

        walletAssetRepository.findByVaultIdAndCurrencyAndProtocol(command.vaultId, command.currency, command.protocol)?.let {
            log.info("Asset already active: vaultId={}, currency={}, protocol={}", command.vaultId, command.currency, command.protocol)
            return it
        }

        val supportedAsset =
            supportedAssetRepository.findByCurrencyAndProtocol(command.currency, command.protocol)
                ?: throw AssetNotFoundException(command.vaultId.value.toString(), "${command.currency}/${command.protocol}")

        val fireblocksVaultId =
            vault.fireblocksVaultId
                ?: throw IllegalStateException("Active vault ${vault.id} has no fireblocksVaultId")
        fireblocksClient.createWalletAsset(fireblocksVaultId, supportedAsset.fireblocksAssetId)

        val walletAsset = WalletAsset.create(command.vaultId, command.currency, command.protocol, supportedAsset.fireblocksAssetId)
        val saved = walletAssetRepository.save(walletAsset)

        assetEventPublisher.publish(WalletAssetCreatedEvent.from(saved))
        auditLogRepository.save(
            AuditLog.create(
                operation = AuditOperation.ASSET_ACTIVATED,
                actor = "system",
                resourceId = saved.id.value.toString(),
                status = AuditStatus.SUCCESS,
            ),
        )

        return saved
    }

    @Transactional
    fun generateDepositAddress(command: GenerateAddressCommand): DepositAddress {
        val walletAsset =
            walletAssetRepository.findByVaultIdAndCurrencyAndProtocol(command.vaultId, command.currency, command.protocol)
                ?: throw AssetNotFoundException(command.vaultId.value.toString(), "${command.currency}/${command.protocol}")

        depositAddressRepository.findByWalletAssetId(walletAsset.id)?.let {
            log.info("Address already exists: walletAssetId={}", walletAsset.id)
            return it
        }

        val vault =
            vaultRepository.findById(command.vaultId)
                ?: throw VaultNotFoundException(command.vaultId.value.toString())
        val fireblocksVaultId =
            vault.fireblocksVaultId
                ?: throw IllegalStateException("Active vault ${vault.id} has no fireblocksVaultId")
        val fbAddress = fireblocksClient.generateDepositAddress(fireblocksVaultId, walletAsset.fireblocksAssetId)

        val address =
            DepositAddress.create(
                walletAssetId = walletAsset.id,
                address = fbAddress.address,
                tag = fbAddress.tag,
                legacyAddress = fbAddress.legacyAddress,
            )
        val saved = depositAddressRepository.save(address)

        addressEventPublisher.publish(AddressCreatedEvent.from(saved, command.vaultId))
        auditLogRepository.save(
            AuditLog.create(
                operation = AuditOperation.ADDRESS_GENERATED,
                actor = "system",
                resourceId = saved.id.value.toString(),
                status = AuditStatus.SUCCESS,
            ),
        )

        return saved
    }
}
