package com.stablecoin.custody.fireblocks.infrastructure.persistence

import com.stablecoin.custody.fireblocks.AbstractIntegrationTest
import com.stablecoin.custody.fireblocks.domain.vault.VaultRepository
import com.stablecoin.custody.fireblocks.domain.wallet.WalletAssetId
import com.stablecoin.custody.fireblocks.domain.wallet.WalletAssetRepository
import com.stablecoin.custody.fireblocks.test.fixtures.aVault
import com.stablecoin.custody.fireblocks.test.fixtures.aWalletAsset
import jakarta.persistence.EntityManager
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Transactional
class WalletAssetPersistenceIntegrationTest : AbstractIntegrationTest() {
    @Autowired
    private lateinit var walletAssetRepository: WalletAssetRepository

    @Autowired
    private lateinit var vaultRepository: VaultRepository

    @Autowired
    private lateinit var entityManager: EntityManager

    @Test
    fun `should save and retrieve wallet asset`() {
        // given
        val vault = vaultRepository.save(aVault())
        val walletAsset = aWalletAsset(vaultId = vault.id)
        val saved = walletAssetRepository.save(walletAsset)
        entityManager.clear()

        // when
        val result = walletAssetRepository.findById(saved.id)

        // then
        assertThat(result)
            .usingRecursiveComparison()
            .ignoringFields("createdAt", "updatedAt")
            .isEqualTo(saved)
    }

    @Test
    fun `should find wallet asset by vault ID, currency, and protocol`() {
        // given
        val vault = vaultRepository.save(aVault())
        val walletAsset = aWalletAsset(vaultId = vault.id, currency = "ETH", protocol = "ETH", fireblocksAssetId = "ETH")
        val saved = walletAssetRepository.save(walletAsset)
        entityManager.clear()

        // when
        val result = walletAssetRepository.findByVaultIdAndCurrencyAndProtocol(vault.id, "ETH", "ETH")

        // then
        assertThat(result)
            .usingRecursiveComparison()
            .ignoringFields("createdAt", "updatedAt")
            .isEqualTo(saved)
    }

    @Test
    fun `should enforce composite unique constraint on (vault_id, currency, protocol)`() {
        // given
        val vault = vaultRepository.save(aVault())
        walletAssetRepository.save(aWalletAsset(vaultId = vault.id, currency = "BTC", protocol = "BTC", fireblocksAssetId = "BTC"))

        // when/then
        assertThatThrownBy {
            walletAssetRepository.save(aWalletAsset(vaultId = vault.id, currency = "BTC", protocol = "BTC", fireblocksAssetId = "BTC"))
        }.isInstanceOf(DataIntegrityViolationException::class.java)
    }

    @Test
    fun `should return null when wallet asset not found`() {
        // when
        val result = walletAssetRepository.findById(WalletAssetId(UUID.randomUUID()))

        // then
        assertThat(result).isNull()
    }
}
