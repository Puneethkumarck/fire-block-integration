package com.stablecoin.custody.fireblocks.domain.vault

import com.stablecoin.custody.fireblocks.domain.exception.VaultNotFoundException
import com.stablecoin.custody.fireblocks.test.fixtures.aVault
import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class VaultQueryServiceTest {
    private val vaultRepository: VaultRepository = mockk()

    private val service = VaultQueryService(vaultRepository)

    @Test
    fun `should return vault when found by ID`() {
        // given
        val vault = aVault()
        every { vaultRepository.findById(vault.id) } returns vault

        // when
        val result = service.getVault(vault.id)

        // then
        assertThat(result).usingRecursiveComparison().isEqualTo(vault)
    }

    @Test
    fun `should throw VaultNotFoundException when not found by ID`() {
        // given
        val vaultId = VaultId(java.util.UUID.randomUUID())
        every { vaultRepository.findById(vaultId) } returns null

        // when/then
        assertThatThrownBy { service.getVault(vaultId) }
            .isInstanceOf(VaultNotFoundException::class.java)
            .hasMessageContaining(vaultId.value.toString())
    }

    @Test
    fun `should return vault when found by customerRefId`() {
        // given
        val vault = aVault(customerRefId = "customer-ref-001")
        every { vaultRepository.findByCustomerRefId("customer-ref-001") } returns vault

        // when
        val result = service.getVaultByCustomerRefId("customer-ref-001")

        // then
        assertThat(result).usingRecursiveComparison().isEqualTo(vault)
    }

    @Test
    fun `should throw VaultNotFoundException when not found by customerRefId`() {
        // given
        every { vaultRepository.findByCustomerRefId("unknown-ref") } returns null

        // when/then
        assertThatThrownBy { service.getVaultByCustomerRefId("unknown-ref") }
            .isInstanceOf(VaultNotFoundException::class.java)
            .hasMessageContaining("unknown-ref")
    }
}
