package com.stablecoin.custody.fireblocks.infrastructure.secrets

import com.stablecoin.custody.fireblocks.infrastructure.secrets.SecretsConfiguration.Companion.parsePrivateKey
import com.stablecoin.custody.fireblocks.infrastructure.secrets.SecretsConfiguration.Companion.parsePublicKey
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.security.KeyPairGenerator
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.util.Base64

class SecretsConfigurationTest {
    private val keyPair = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()

    @Test
    fun `should parse PKCS8 DER private key from Base64`() {
        // given
        val base64Key = Base64.getEncoder().encodeToString(keyPair.private.encoded)

        // when
        val result = parsePrivateKey("test/private-key", base64Key)

        // then
        assertThat(result).isInstanceOf(RSAPrivateKey::class.java)
        assertThat(result.algorithm).isEqualTo("RSA")
        assertThat(result.encoded).isEqualTo(keyPair.private.encoded)
    }

    @Test
    fun `should parse X509 DER public key from Base64`() {
        // given
        val base64Key = Base64.getEncoder().encodeToString(keyPair.public.encoded)

        // when
        val result = parsePublicKey("test/public-key", base64Key)

        // then
        assertThat(result).isInstanceOf(RSAPublicKey::class.java)
        assertThat(result.algorithm).isEqualTo("RSA")
        assertThat(result.encoded).isEqualTo(keyPair.public.encoded)
    }

    @Test
    fun `should throw on invalid Base64 for private key`() {
        // when/then
        assertThatThrownBy { parsePrivateKey("test/private-key", "not-valid-base64!!!") }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessageContaining("test/private-key")
    }

    @Test
    fun `should throw on invalid key format for private key`() {
        // given
        val invalidKeyBytes = Base64.getEncoder().encodeToString("not-a-real-key".toByteArray())

        // when/then
        assertThatThrownBy { parsePrivateKey("test/private-key", invalidKeyBytes) }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessageContaining("test/private-key")
    }

    @Test
    fun `should throw on invalid Base64 for public key`() {
        // when/then
        assertThatThrownBy { parsePublicKey("test/public-key", "not-valid-base64!!!") }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessageContaining("test/public-key")
    }

    @Test
    fun `should throw on invalid key format for public key`() {
        // given
        val invalidKeyBytes = Base64.getEncoder().encodeToString("not-a-real-key".toByteArray())

        // when/then
        assertThatThrownBy { parsePublicKey("test/public-key", invalidKeyBytes) }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessageContaining("test/public-key")
    }
}
