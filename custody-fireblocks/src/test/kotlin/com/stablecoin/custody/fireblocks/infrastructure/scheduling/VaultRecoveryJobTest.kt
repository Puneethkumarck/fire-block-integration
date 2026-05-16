package com.stablecoin.custody.fireblocks.infrastructure.scheduling

import com.stablecoin.custody.fireblocks.domain.port.FireblocksVaultPort
import com.stablecoin.custody.fireblocks.domain.port.VaultResult
import com.stablecoin.custody.fireblocks.domain.vault.VaultRepository
import com.stablecoin.custody.fireblocks.domain.vault.VaultStatus
import com.stablecoin.custody.fireblocks.test.fixtures.aVault
import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class VaultRecoveryJobTest {
    private val vaultRepository: VaultRepository = mockk()
    private val fireblocksVaultPort: FireblocksVaultPort = mockk()

    private val job = VaultRecoveryJob(vaultRepository, fireblocksVaultPort)

    @Test
    fun `should recover pending vaults by creating them on Fireblocks`() {
        // given
        val vault = aVault(status = VaultStatus.PENDING, fireblocksVaultId = null)
        every { vaultRepository.findPendingOlderThan(any()) } returns listOf(vault)
        every { fireblocksVaultPort.createVault(vault.name, vault.customerRefId) } returns
            VaultResult(id = "fb-vault-999", name = vault.name)
        every { vaultRepository.save(any()) } returnsArgument 0

        // when
        job.recoverPendingVaults()

        // then
        verify {
            vaultRepository.save(
                match { it.status == VaultStatus.ACTIVE && it.fireblocksVaultId == "fb-vault-999" },
            )
        }
    }

    @Test
    fun `should mark vault as failed when recovery fails`() {
        // given
        val vault = aVault(status = VaultStatus.PENDING, fireblocksVaultId = null)
        every { vaultRepository.findPendingOlderThan(any()) } returns listOf(vault)
        every { fireblocksVaultPort.createVault(vault.name, vault.customerRefId) } throws
            RuntimeException("API error")
        every { vaultRepository.save(any()) } returnsArgument 0

        // when
        job.recoverPendingVaults()

        // then
        verify {
            vaultRepository.save(match { it.status == VaultStatus.FAILED })
        }
    }

    @Test
    fun `should not process when no pending vaults exist`() {
        // given
        every { vaultRepository.findPendingOlderThan(any()) } returns emptyList()

        // when
        job.recoverPendingVaults()

        // then
        verify(exactly = 0) { fireblocksVaultPort.createVault(any(), any()) }
    }

    @Test
    fun `should continue processing when single vault recovery fails`() {
        // given
        val vault1 = aVault(status = VaultStatus.PENDING, fireblocksVaultId = null, name = "Vault 1")
        val vault2 = aVault(status = VaultStatus.PENDING, fireblocksVaultId = null, name = "Vault 2")
        every { vaultRepository.findPendingOlderThan(any()) } returns listOf(vault1, vault2)
        every { fireblocksVaultPort.createVault(vault1.name, vault1.customerRefId) } throws
            RuntimeException("API error")
        every { fireblocksVaultPort.createVault(vault2.name, vault2.customerRefId) } returns
            VaultResult(id = "fb-vault-002", name = vault2.name)
        every { vaultRepository.save(any()) } returnsArgument 0

        // when
        job.recoverPendingVaults()

        // then
        verify {
            vaultRepository.save(match { it.status == VaultStatus.FAILED })
            vaultRepository.save(
                match { it.status == VaultStatus.ACTIVE && it.fireblocksVaultId == "fb-vault-002" },
            )
        }
    }
}
