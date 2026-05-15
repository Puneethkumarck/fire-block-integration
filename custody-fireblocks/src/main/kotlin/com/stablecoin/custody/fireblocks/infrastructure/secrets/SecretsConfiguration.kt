package com.stablecoin.custody.fireblocks.infrastructure.secrets

import com.stablecoin.custody.fireblocks.infrastructure.fireblocks.config.FireblocksProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest
import java.security.KeyFactory
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.Base64

@Configuration
class SecretsConfiguration(
    private val properties: FireblocksProperties,
) {
    @Bean
    fun fireblocksPrivateKey(secretsManagerClient: SecretsManagerClient): RSAPrivateKey {
        val secretValue = getSecretValue(secretsManagerClient, properties.secrets.privateKeyName)
        return parsePrivateKey(properties.secrets.privateKeyName, secretValue)
    }

    @Bean
    fun fireblocksApiKey(secretsManagerClient: SecretsManagerClient): String =
        getSecretValue(secretsManagerClient, properties.secrets.apiKeyName)

    @Bean
    fun fireblocksWebhookPublicKey(secretsManagerClient: SecretsManagerClient): RSAPublicKey {
        val secretValue = getSecretValue(secretsManagerClient, properties.secrets.webhookPublicKeyName)
        return parsePublicKey(properties.secrets.webhookPublicKeyName, secretValue)
    }

    companion object {
        private val keyFactory = KeyFactory.getInstance("RSA")

        private fun getSecretValue(
            client: SecretsManagerClient,
            secretName: String,
        ): String {
            val request =
                GetSecretValueRequest
                    .builder()
                    .secretId(secretName)
                    .build()
            return client.getSecretValue(request).secretString()
        }

        fun parsePrivateKey(
            secretName: String,
            base64EncodedKey: String,
        ): RSAPrivateKey =
            try {
                val keyBytes = Base64.getDecoder().decode(base64EncodedKey)
                val keySpec = PKCS8EncodedKeySpec(keyBytes)
                keyFactory.generatePrivate(keySpec) as RSAPrivateKey
            } catch (e: Exception) {
                throw IllegalStateException("Failed to parse PKCS#8 private key from secret '$secretName'", e)
            }

        fun parsePublicKey(
            secretName: String,
            base64EncodedKey: String,
        ): RSAPublicKey =
            try {
                val keyBytes = Base64.getDecoder().decode(base64EncodedKey)
                val keySpec = X509EncodedKeySpec(keyBytes)
                keyFactory.generatePublic(keySpec) as RSAPublicKey
            } catch (e: Exception) {
                throw IllegalStateException("Failed to parse X.509 public key from secret '$secretName'", e)
            }
    }
}
