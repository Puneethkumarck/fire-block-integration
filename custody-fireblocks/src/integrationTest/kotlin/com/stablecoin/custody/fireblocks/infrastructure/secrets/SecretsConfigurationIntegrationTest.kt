package com.stablecoin.custody.fireblocks.infrastructure.secrets

import com.stablecoin.custody.fireblocks.AbstractIntegrationTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey

class SecretsConfigurationIntegrationTest : AbstractIntegrationTest() {
    @Autowired
    private lateinit var fireblocksPrivateKey: RSAPrivateKey

    @Autowired
    @Qualifier("fireblocksApiKey")
    private lateinit var fireblocksApiKey: String

    @Autowired
    private lateinit var fireblocksWebhookPublicKey: RSAPublicKey

    @Test
    fun `should load RSAPrivateKey bean from LocalStack`() {
        // then
        assertThat(fireblocksPrivateKey).isNotNull
        assertThat(fireblocksPrivateKey.algorithm).isEqualTo("RSA")
        assertThat(fireblocksPrivateKey.format).isEqualTo("PKCS#8")
    }

    @Test
    fun `should load API key bean from LocalStack`() {
        // then
        assertThat(fireblocksApiKey).isEqualTo("test-api-key")
    }

    @Test
    fun `should load RSAPublicKey bean from LocalStack`() {
        // then
        assertThat(fireblocksWebhookPublicKey).isNotNull
        assertThat(fireblocksWebhookPublicKey.algorithm).isEqualTo("RSA")
        assertThat(fireblocksWebhookPublicKey.format).isEqualTo("X.509")
    }

    @Test
    fun `should load matching key pair from LocalStack`() {
        // then
        assertThat(fireblocksPrivateKey.modulus).isEqualTo(fireblocksWebhookPublicKey.modulus)
    }
}
