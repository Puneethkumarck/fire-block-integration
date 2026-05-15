package com.stablecoin.custody.fireblocks.domain.wallet

import com.stablecoin.custody.fireblocks.test.fixtures.aSupportedAsset
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class SupportedAssetTest {
    @Test
    fun `should create supported asset with all fields`() {
        // given
        val currency = "ETH"
        val protocol = "ETH"
        val fireblocksAssetId = "ETH"

        // when
        val result = aSupportedAsset(currency = currency, protocol = protocol, fireblocksAssetId = fireblocksAssetId)

        // then
        val expected = aSupportedAsset(currency = currency, protocol = protocol, fireblocksAssetId = fireblocksAssetId)
        assertThat(result)
            .usingRecursiveComparison()
            .ignoringFields("id", "createdAt")
            .isEqualTo(expected)
    }

    @Test
    fun `should reject blank currency`() {
        // when/then
        assertThatThrownBy { aSupportedAsset(currency = " ") }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("currency must not be blank")
    }

    @Test
    fun `should reject blank protocol`() {
        // when/then
        assertThatThrownBy { aSupportedAsset(protocol = " ") }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("protocol must not be blank")
    }

    @Test
    fun `should reject blank fireblocksAssetId`() {
        // when/then
        assertThatThrownBy { aSupportedAsset(fireblocksAssetId = " ") }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("fireblocksAssetId must not be blank")
    }
}
