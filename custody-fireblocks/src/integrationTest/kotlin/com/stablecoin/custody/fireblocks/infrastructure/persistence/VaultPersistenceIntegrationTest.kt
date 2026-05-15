package com.stablecoin.custody.fireblocks.infrastructure.persistence

import com.stablecoin.custody.fireblocks.AbstractIntegrationTest
import com.stablecoin.custody.fireblocks.domain.vault.VaultId
import com.stablecoin.custody.fireblocks.domain.vault.VaultRepository
import com.stablecoin.custody.fireblocks.domain.vault.VaultStatus
import com.stablecoin.custody.fireblocks.test.fixtures.aVault
import jakarta.persistence.EntityManager
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

@Transactional
class VaultPersistenceIntegrationTest : AbstractIntegrationTest() {
    @Autowired
    private lateinit var vaultRepository: VaultRepository

    @Autowired
    private lateinit var entityManager: EntityManager

    @Test
    fun `should save and retrieve vault by ID`() {
        // given
        val vault = aVault(status = VaultStatus.PENDING, fireblocksVaultId = null)
        val saved = vaultRepository.save(vault)
        entityManager.clear()

        // when
        val result = vaultRepository.findById(saved.id)

        // then
        assertThat(result)
            .usingRecursiveComparison()
            .ignoringFields("createdAt", "updatedAt")
            .isEqualTo(saved)
    }

    @Test
    fun `should find vault by customerRefId`() {
        // given
        val vault = aVault(customerRefId = "unique-ref-${UUID.randomUUID()}")
        val saved = vaultRepository.save(vault)
        entityManager.clear()

        // when
        val result = vaultRepository.findByCustomerRefId(saved.customerRefId)

        // then
        assertThat(result)
            .usingRecursiveComparison()
            .ignoringFields("createdAt", "updatedAt")
            .isEqualTo(saved)
    }

    @Test
    fun `should return null when vault not found`() {
        // when
        val result = vaultRepository.findById(VaultId(UUID.randomUUID()))

        // then
        assertThat(result).isNull()
    }

    @Test
    fun `should enforce unique customerRefId constraint`() {
        // given
        val customerRefId = "duplicate-ref-${UUID.randomUUID()}"
        vaultRepository.save(aVault(customerRefId = customerRefId))

        // when/then
        assertThatThrownBy {
            vaultRepository.save(aVault(customerRefId = customerRefId))
        }.isInstanceOf(DataIntegrityViolationException::class.java)
    }

    @Test
    fun `should update vault status with optimistic locking`() {
        // given
        val vault = aVault(status = VaultStatus.PENDING, fireblocksVaultId = null)
        val saved = vaultRepository.save(vault)
        entityManager.clear()

        // when
        val activated = saved.activate("fb-vault-789")
        val result = vaultRepository.save(activated)

        // then
        val expected =
            saved.copy(
                fireblocksVaultId = "fb-vault-789",
                status = VaultStatus.ACTIVE,
                version = saved.version + 1,
            )
        assertThat(result)
            .usingRecursiveComparison()
            .ignoringFields("createdAt", "updatedAt")
            .isEqualTo(expected)
    }

    @Test
    fun `should find pending vaults older than cutoff`() {
        // given
        val oldVault =
            aVault(
                status = VaultStatus.PENDING,
                fireblocksVaultId = null,
                createdAt = Instant.now().minus(2, ChronoUnit.HOURS),
            )
        val recentVault =
            aVault(
                status = VaultStatus.PENDING,
                fireblocksVaultId = null,
                createdAt = Instant.now(),
            )
        val activeVault =
            aVault(
                status = VaultStatus.ACTIVE,
                createdAt = Instant.now().minus(2, ChronoUnit.HOURS),
            )
        val savedOld = vaultRepository.save(oldVault)
        vaultRepository.save(recentVault)
        vaultRepository.save(activeVault)
        entityManager.clear()

        // when
        val cutoff = Instant.now().minus(1, ChronoUnit.HOURS)
        val result = vaultRepository.findPendingOlderThan(cutoff)

        // then
        assertThat(result).hasSize(1)
        assertThat(result[0])
            .usingRecursiveComparison()
            .ignoringFields("createdAt", "updatedAt")
            .isEqualTo(savedOld)
    }
}
