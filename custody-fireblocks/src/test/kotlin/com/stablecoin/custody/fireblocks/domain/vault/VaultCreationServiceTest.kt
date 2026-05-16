package com.stablecoin.custody.fireblocks.domain.vault

import com.stablecoin.custody.fireblocks.domain.audit.AuditLogRepository
import com.stablecoin.custody.fireblocks.domain.audit.AuditOperation
import com.stablecoin.custody.fireblocks.domain.audit.AuditStatus
import com.stablecoin.custody.fireblocks.domain.event.VaultCreatedEvent
import com.stablecoin.custody.fireblocks.domain.port.EventPublisher
import com.stablecoin.custody.fireblocks.domain.port.FireblocksVaultPort
import com.stablecoin.custody.fireblocks.test.fixtures.aCreateVaultCommand
import com.stablecoin.custody.fireblocks.test.fixtures.aVault
import com.stablecoin.custody.fireblocks.test.fixtures.aVaultResult
import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class VaultCreationServiceTest {
    private val vaultRepository: VaultRepository = mockk()
    private val fireblocksClient: FireblocksVaultPort = mockk()
    private val eventPublisher: EventPublisher<VaultCreatedEvent> = mockk()
    private val auditLogRepository: AuditLogRepository = mockk()

    private val service = VaultCreationService(vaultRepository, fireblocksClient, eventPublisher, auditLogRepository)

    @Test
    fun `should create vault and publish event`() {
        // given
        val command = aCreateVaultCommand()
        val vaultResult = aVaultResult()
        every { vaultRepository.findByCustomerRefId(command.customerRefId) } returns null
        every { vaultRepository.save(any()) } returnsArgument 0
        every { fireblocksClient.createVault(command.name, command.customerRefId) } returns vaultResult
        every { eventPublisher.publish(any()) } just runs
        every { auditLogRepository.save(any()) } returnsArgument 0

        // when
        val result = service.createVault(command)

        // then
        val expected =
            aVault(
                customerRefId = command.customerRefId,
                name = command.name,
                fireblocksVaultId = vaultResult.id,
                status = VaultStatus.ACTIVE,
            )
        assertThat(result)
            .usingRecursiveComparison()
            .ignoringFields("id", "createdAt", "updatedAt")
            .isEqualTo(expected)
        verify { eventPublisher.publish(any<VaultCreatedEvent>()) }
    }

    @Test
    fun `should return existing vault when customerRefId already exists`() {
        // given
        val command = aCreateVaultCommand()
        val existingVault = aVault(customerRefId = command.customerRefId)
        every { vaultRepository.findByCustomerRefId(command.customerRefId) } returns existingVault

        // when
        val result = service.createVault(command)

        // then
        assertThat(result).usingRecursiveComparison().isEqualTo(existingVault)
        verify(exactly = 0) { fireblocksClient.createVault(any(), any()) }
    }

    @Test
    fun `should save audit log on successful creation`() {
        // given
        val command = aCreateVaultCommand()
        val vaultResult = aVaultResult()
        every { vaultRepository.findByCustomerRefId(command.customerRefId) } returns null
        every { vaultRepository.save(any()) } returnsArgument 0
        every { fireblocksClient.createVault(command.name, command.customerRefId) } returns vaultResult
        every { eventPublisher.publish(any()) } just runs
        every { auditLogRepository.save(any()) } returnsArgument 0

        // when
        service.createVault(command)

        // then
        verify {
            auditLogRepository.save(
                match {
                    it.operation == AuditOperation.VAULT_CREATED &&
                        it.status == AuditStatus.SUCCESS
                },
            )
        }
    }

    @Test
    fun `should leave vault in PENDING when Fireblocks call fails`() {
        // given
        val command = aCreateVaultCommand()
        every { vaultRepository.findByCustomerRefId(command.customerRefId) } returns null
        every { vaultRepository.save(any()) } returnsArgument 0
        every { fireblocksClient.createVault(command.name, command.customerRefId) } throws RuntimeException("Fireblocks unavailable")
        every { auditLogRepository.save(any()) } returnsArgument 0

        // when
        val result = service.createVault(command)

        // then
        val expected =
            aVault(
                customerRefId = command.customerRefId,
                name = command.name,
                fireblocksVaultId = null,
                status = VaultStatus.PENDING,
            )
        assertThat(result)
            .usingRecursiveComparison()
            .ignoringFields("id", "createdAt", "updatedAt")
            .isEqualTo(expected)
    }

    @Test
    fun `should save audit log with FAILURE status when Fireblocks call fails`() {
        // given
        val command = aCreateVaultCommand()
        every { vaultRepository.findByCustomerRefId(command.customerRefId) } returns null
        every { vaultRepository.save(any()) } returnsArgument 0
        every { fireblocksClient.createVault(command.name, command.customerRefId) } throws RuntimeException("Fireblocks unavailable")
        every { auditLogRepository.save(any()) } returnsArgument 0

        // when
        service.createVault(command)

        // then
        verify {
            auditLogRepository.save(
                match {
                    it.operation == AuditOperation.VAULT_CREATION_FAILED &&
                        it.status == AuditStatus.FAILURE
                },
            )
        }
    }

    @Test
    fun `should call Fireblocks with correct name and customerRefId`() {
        // given
        val command = aCreateVaultCommand(customerRefId = "custom-ref-123", name = "My Vault")
        val vaultResult = aVaultResult()
        every { vaultRepository.findByCustomerRefId(command.customerRefId) } returns null
        every { vaultRepository.save(any()) } returnsArgument 0
        every { fireblocksClient.createVault(command.name, command.customerRefId) } returns vaultResult
        every { eventPublisher.publish(any()) } just runs
        every { auditLogRepository.save(any()) } returnsArgument 0

        // when
        service.createVault(command)

        // then
        verify { fireblocksClient.createVault("My Vault", "custom-ref-123") }
    }
}
