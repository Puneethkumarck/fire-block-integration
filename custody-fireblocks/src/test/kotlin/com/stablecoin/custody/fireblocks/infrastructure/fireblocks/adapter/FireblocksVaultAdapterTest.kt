package com.stablecoin.custody.fireblocks.infrastructure.fireblocks.adapter

import com.stablecoin.custody.fireblocks.domain.port.DepositAddressResult
import com.stablecoin.custody.fireblocks.domain.port.VaultResult
import com.stablecoin.custody.fireblocks.domain.port.WalletAssetResult
import com.stablecoin.custody.fireblocks.infrastructure.fireblocks.client.FireblocksVaultClient
import com.stablecoin.custody.fireblocks.infrastructure.fireblocks.client.dto.CreateVaultAccountRequest
import com.stablecoin.custody.fireblocks.infrastructure.fireblocks.client.dto.FireblocksDepositAddressResponse
import com.stablecoin.custody.fireblocks.infrastructure.fireblocks.client.dto.FireblocksVaultAccountResponse
import com.stablecoin.custody.fireblocks.infrastructure.fireblocks.client.dto.FireblocksWalletAssetResponse
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class FireblocksVaultAdapterTest {
    private val vaultClient = mockk<FireblocksVaultClient>()
    private val adapter = FireblocksVaultAdapter(vaultClient)

    @Test
    fun `should create vault and return domain response`() {
        // given
        val response = FireblocksVaultAccountResponse(id = "fb-123", name = "Test Vault", customerRefId = "cust-001")
        every {
            vaultClient.createVault(CreateVaultAccountRequest(name = "Test Vault", customerRefId = "cust-001"))
        } returns response

        // when
        val result = adapter.createVault("Test Vault", "cust-001")

        // then
        val expected = VaultResult(id = "fb-123", name = "Test Vault")
        assertThat(result).usingRecursiveComparison().isEqualTo(expected)
        verify { vaultClient.createVault(CreateVaultAccountRequest(name = "Test Vault", customerRefId = "cust-001")) }
    }

    @Test
    fun `should get vault and map to domain response`() {
        // given
        val response = FireblocksVaultAccountResponse(id = "fb-456", name = "My Vault")
        every { vaultClient.getVault("fb-456") } returns response

        // when
        val result = adapter.getVault("fb-456")

        // then
        val expected = VaultResult(id = "fb-456", name = "My Vault")
        assertThat(result).usingRecursiveComparison().isEqualTo(expected)
    }

    @Test
    fun `should create wallet asset and return domain response`() {
        // given
        val response = FireblocksWalletAssetResponse(id = "ETH_TEST", available = "0")
        every { vaultClient.createWalletAsset("fb-123", "ETH_TEST") } returns response

        // when
        val result = adapter.createWalletAsset("fb-123", "ETH_TEST")

        // then
        val expected = WalletAssetResult(id = "ETH_TEST", available = "0")
        assertThat(result).usingRecursiveComparison().isEqualTo(expected)
    }

    @Test
    fun `should generate deposit address and return domain response`() {
        // given
        val response = FireblocksDepositAddressResponse(address = "0xabc123", tag = "memo-tag", legacyAddress = null)
        every { vaultClient.generateDepositAddress("fb-123", "ETH_TEST", null) } returns response

        // when
        val result = adapter.generateDepositAddress("fb-123", "ETH_TEST")

        // then
        val expected = DepositAddressResult(address = "0xabc123", tag = "memo-tag", legacyAddress = null)
        assertThat(result).usingRecursiveComparison().isEqualTo(expected)
    }

    @Test
    fun `should map null tag as null in deposit address`() {
        // given
        val response = FireblocksDepositAddressResponse(address = "0xdef456")
        every { vaultClient.generateDepositAddress("fb-123", "BTC_TEST", null) } returns response

        // when
        val result = adapter.generateDepositAddress("fb-123", "BTC_TEST")

        // then
        val expected = DepositAddressResult(address = "0xdef456", tag = null, legacyAddress = null)
        assertThat(result).usingRecursiveComparison().isEqualTo(expected)
    }
}
