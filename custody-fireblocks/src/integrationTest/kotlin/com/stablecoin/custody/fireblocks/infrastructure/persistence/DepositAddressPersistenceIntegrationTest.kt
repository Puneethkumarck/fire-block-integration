package com.stablecoin.custody.fireblocks.infrastructure.persistence

import com.stablecoin.custody.fireblocks.AbstractIntegrationTest
import com.stablecoin.custody.fireblocks.domain.vault.VaultRepository
import com.stablecoin.custody.fireblocks.domain.wallet.DepositAddressRepository
import com.stablecoin.custody.fireblocks.domain.wallet.WalletAssetId
import com.stablecoin.custody.fireblocks.domain.wallet.WalletAssetRepository
import com.stablecoin.custody.fireblocks.test.fixtures.aDepositAddress
import com.stablecoin.custody.fireblocks.test.fixtures.aVault
import com.stablecoin.custody.fireblocks.test.fixtures.aWalletAsset
import jakarta.persistence.EntityManager
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Transactional
class DepositAddressPersistenceIntegrationTest : AbstractIntegrationTest() {
    @Autowired
    private lateinit var depositAddressRepository: DepositAddressRepository

    @Autowired
    private lateinit var walletAssetRepository: WalletAssetRepository

    @Autowired
    private lateinit var vaultRepository: VaultRepository

    @Autowired
    private lateinit var entityManager: EntityManager

    @Test
    fun `should save and retrieve deposit address`() {
        // given
        val vault = vaultRepository.save(aVault())
        val walletAsset = walletAssetRepository.save(aWalletAsset(vaultId = vault.id))
        val depositAddress = aDepositAddress(walletAssetId = walletAsset.id, tag = "memo-1", legacyAddress = "legacy-1")
        val saved = depositAddressRepository.save(depositAddress)
        entityManager.clear()

        // when
        val result = depositAddressRepository.findByWalletAssetId(walletAsset.id)

        // then
        assertThat(result)
            .usingRecursiveComparison()
            .ignoringFields("createdAt", "updatedAt")
            .isEqualTo(saved)
    }

    @Test
    fun `should find deposit address by wallet asset ID`() {
        // given
        val vault = vaultRepository.save(aVault())
        val walletAsset = walletAssetRepository.save(aWalletAsset(vaultId = vault.id))
        val saved = depositAddressRepository.save(aDepositAddress(walletAssetId = walletAsset.id))
        entityManager.clear()

        // when
        val result = depositAddressRepository.findByWalletAssetId(walletAsset.id)

        // then
        assertThat(result)
            .usingRecursiveComparison()
            .ignoringFields("createdAt", "updatedAt")
            .isEqualTo(saved)
    }

    @Test
    fun `should persist nullable tag and legacyAddress`() {
        // given
        val vault = vaultRepository.save(aVault())
        val walletAsset = walletAssetRepository.save(aWalletAsset(vaultId = vault.id))
        val depositAddress = aDepositAddress(walletAssetId = walletAsset.id, tag = null, legacyAddress = null)
        val saved = depositAddressRepository.save(depositAddress)
        entityManager.clear()

        // when
        val result = depositAddressRepository.findByWalletAssetId(walletAsset.id)

        // then
        assertThat(result)
            .usingRecursiveComparison()
            .ignoringFields("createdAt", "updatedAt")
            .isEqualTo(saved)
    }

    @Test
    fun `should return null when deposit address not found`() {
        // when
        val result = depositAddressRepository.findByWalletAssetId(WalletAssetId(UUID.randomUUID()))

        // then
        assertThat(result).isNull()
    }
}
