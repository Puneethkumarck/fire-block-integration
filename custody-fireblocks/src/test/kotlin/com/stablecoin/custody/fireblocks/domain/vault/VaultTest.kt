package com.stablecoin.custody.fireblocks.domain.vault

import com.stablecoin.custody.fireblocks.domain.exception.VaultNotActiveException
import com.stablecoin.custody.fireblocks.test.fixtures.aCreateVaultCommand
import com.stablecoin.custody.fireblocks.test.fixtures.aVault
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class VaultTest {
    @Test
    fun `should create vault in PENDING status`() {
        // given
        val command = aCreateVaultCommand()

        // when
        val result = Vault.create(command)

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
    fun `should activate vault with Fireblocks ID`() {
        // given
        val vault = aVault(status = VaultStatus.PENDING, fireblocksVaultId = null)

        // when
        val result = vault.activate("fireblocks-vault-456")

        // then
        val expected =
            vault.copy(
                fireblocksVaultId = "fireblocks-vault-456",
                status = VaultStatus.ACTIVE,
            )
        assertThat(result)
            .usingRecursiveComparison()
            .ignoringFields("updatedAt")
            .isEqualTo(expected)
    }

    @Test
    fun `should mark vault as failed`() {
        // given
        val vault = aVault(status = VaultStatus.PENDING, fireblocksVaultId = null)

        // when
        val result = vault.markFailed()

        // then
        val expected = vault.copy(status = VaultStatus.FAILED)
        assertThat(result)
            .usingRecursiveComparison()
            .ignoringFields("updatedAt")
            .isEqualTo(expected)
    }

    @Test
    fun `should reject blank customerRefId`() {
        // when/then
        assertThatThrownBy { aVault(customerRefId = " ") }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("customerRefId must not be blank")
    }

    @Test
    fun `should reject blank name`() {
        // when/then
        assertThatThrownBy { aVault(name = "") }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("name must not be blank")
    }

    @Test
    fun `should throw VaultNotActiveException when assertActive on PENDING vault`() {
        // given
        val vault = aVault(status = VaultStatus.PENDING)

        // when/then
        assertThatThrownBy { vault.assertActive() }
            .isInstanceOf(VaultNotActiveException::class.java)
            .hasMessageContaining("is not active")
    }

    @Test
    fun `should throw VaultNotActiveException when assertActive on FAILED vault`() {
        // given
        val vault = aVault(status = VaultStatus.FAILED)

        // when/then
        assertThatThrownBy { vault.assertActive() }
            .isInstanceOf(VaultNotActiveException::class.java)
            .hasMessageContaining("is not active")
    }
}
