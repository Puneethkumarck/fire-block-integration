package com.stablecoin.custody.fireblocks.domain.vault

import com.stablecoin.custody.fireblocks.domain.audit.AuditLog
import com.stablecoin.custody.fireblocks.domain.audit.AuditLogRepository
import com.stablecoin.custody.fireblocks.domain.audit.AuditOperation
import com.stablecoin.custody.fireblocks.domain.audit.AuditStatus
import com.stablecoin.custody.fireblocks.domain.event.VaultCreatedEvent
import com.stablecoin.custody.fireblocks.domain.port.EventPublisher
import com.stablecoin.custody.fireblocks.domain.port.FireblocksVaultPort
import com.stablecoin.custody.fireblocks.domain.shared.logger
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

private val log = logger<VaultCreationService>()

@Service
class VaultCreationService(
    private val vaultRepository: VaultRepository,
    private val fireblocksClient: FireblocksVaultPort,
    private val eventPublisher: EventPublisher<VaultCreatedEvent>,
    private val auditLogRepository: AuditLogRepository,
) {
    @Transactional
    fun createVault(command: CreateVaultCommand): Vault {
        log.info("Creating vault: customerRefId={}", command.customerRefId)

        vaultRepository.findByCustomerRefId(command.customerRefId)?.let {
            log.info("Vault already exists: customerRefId={}", command.customerRefId)
            return it
        }

        val vault = Vault.create(command)
        val saved = vaultRepository.save(vault)

        return try {
            val fireblocksVault =
                fireblocksClient.createVault(
                    name = command.name,
                    customerRefId = command.customerRefId,
                )

            val activated = saved.activate(fireblocksVault.id)
            val result = vaultRepository.save(activated)

            eventPublisher.publish(VaultCreatedEvent.from(result))
            auditLogRepository.save(
                AuditLog.create(
                    operation = AuditOperation.VAULT_CREATED,
                    actor = "system",
                    resourceId = result.id.value.toString(),
                    status = AuditStatus.SUCCESS,
                    details = mapOf("fireblocksVaultId" to fireblocksVault.id),
                ),
            )

            result
        } catch (e: Exception) {
            log.error("Fireblocks vault creation failed: customerRefId={}", command.customerRefId, e)
            auditLogRepository.save(
                AuditLog.create(
                    operation = AuditOperation.VAULT_CREATION_FAILED,
                    actor = "system",
                    resourceId = saved.id.value.toString(),
                    status = AuditStatus.FAILURE,
                    details = mapOf("error" to (e.message ?: "unknown")),
                ),
            )
            saved
        }
    }
}
