package com.stablecoin.custody.fireblocks.infrastructure.secrets

import com.stablecoin.custody.fireblocks.AbstractIntegrationTest
import com.stablecoin.custody.fireblocks.infrastructure.fireblocks.config.FireblocksProperties
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient
import software.amazon.awssdk.services.secretsmanager.model.ResourceNotFoundException
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.time.Duration

class SecretsConfigurationIntegrationTest : AbstractIntegrationTest() {
    @Autowired
    private lateinit var fireblocksPrivateKey: RSAPrivateKey

    @Autowired
    @Qualifier("fireblocksApiKey")
    private lateinit var fireblocksApiKey: String

    @Autowired
    private lateinit var fireblocksWebhookPublicKey: RSAPublicKey

    @Autowired
    private lateinit var secretsManagerClient: SecretsManagerClient

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

    @Test
    fun `should fail startup if secret not found in Secrets Manager`() {
        // given
        val properties =
            FireblocksProperties(
                api =
                    FireblocksProperties.ApiProperties(
                        baseUrl = "http://localhost",
                        connectTimeout = Duration.ofSeconds(5),
                        readTimeout = Duration.ofSeconds(5),
                    ),
                secrets =
                    FireblocksProperties.SecretsProperties(
                        privateKeyName = "non-existent/private-key",
                        apiKeyName = "fireblocks/api-key",
                        webhookPublicKeyName = "fireblocks/webhook-public-key",
                    ),
            )
        val config = SecretsConfiguration(properties)

        // when/then
        assertThatThrownBy { config.fireblocksPrivateKey(secretsManagerClient) }
            .isInstanceOf(ResourceNotFoundException::class.java)
    }
}
