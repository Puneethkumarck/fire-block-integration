package com.stablecoin.custody.fireblocks.infrastructure.persistence

import com.stablecoin.custody.fireblocks.AbstractIntegrationTest
import com.stablecoin.custody.fireblocks.domain.wallet.SupportedAssetRepository
import com.stablecoin.custody.fireblocks.test.fixtures.aSupportedAsset
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.annotation.Transactional

@Transactional
class SupportedAssetPersistenceIntegrationTest : AbstractIntegrationTest() {
    @Autowired
    private lateinit var supportedAssetRepository: SupportedAssetRepository

    @Test
    fun `should find supported asset by currency and protocol`() {
        // when
        val result = supportedAssetRepository.findByCurrencyAndProtocol("BTC", "BTC")

        // then
        val expected = aSupportedAsset(currency = "BTC", protocol = "BTC", fireblocksAssetId = "BTC", utxo = true)
        assertThat(result).isNotNull
        assertThat(result!!)
            .usingRecursiveComparison()
            .ignoringFields("id", "createdAt")
            .isEqualTo(expected)
    }

    @Test
    fun `should find supported asset by fireblocksAssetId`() {
        // when
        val result = supportedAssetRepository.findByFireblocksAssetId("ETH")

        // then
        val expected = aSupportedAsset(currency = "ETH", protocol = "ETH", fireblocksAssetId = "ETH", utxo = false)
        assertThat(result).isNotNull
        assertThat(result!!)
            .usingRecursiveComparison()
            .ignoringFields("id", "createdAt")
            .isEqualTo(expected)
    }

    @Test
    fun `should return all active supported assets`() {
        // when
        val result = supportedAssetRepository.findAllActive()

        // then
        assertThat(result).hasSize(4)
        assertThat(result.map { it.currency }).containsExactlyInAnyOrder("BTC", "ETH", "SOL", "EURC")
    }

    @Test
    fun `should return null for unsupported currency-protocol pair`() {
        // when
        val result = supportedAssetRepository.findByCurrencyAndProtocol("XRP", "XRP")

        // then
        assertThat(result).isNull()
    }

    @Test
    fun `should have seed data from V8 migration`() {
        // when
        val btc = supportedAssetRepository.findByCurrencyAndProtocol("BTC", "BTC")
        val eth = supportedAssetRepository.findByCurrencyAndProtocol("ETH", "ETH")
        val sol = supportedAssetRepository.findByCurrencyAndProtocol("SOL", "SOL")
        val eurc = supportedAssetRepository.findByCurrencyAndProtocol("EURC", "ETH")

        // then
        assertThat(btc).isNotNull
        assertThat(eth).isNotNull
        assertThat(sol).isNotNull
        assertThat(eurc).isNotNull
        assertThat(btc!!.utxo).isTrue()
        assertThat(eurc!!.fireblocksAssetId).isEqualTo("EURC")
    }
}
