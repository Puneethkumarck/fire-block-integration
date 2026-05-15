package com.stablecoin.custody.fireblocks.infrastructure.secrets

import org.springframework.beans.factory.annotation.Value
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
    @param:Value("\${fireblocks.secrets.private-key-name}") private val privateKeyName: String,
    @param:Value("\${fireblocks.secrets.api-key-name}") private val apiKeyName: String,
    @param:Value("\${fireblocks.secrets.webhook-public-key-name}") private val webhookPublicKeyName: String,
) {
    @Bean
    fun fireblocksPrivateKey(secretsManagerClient: SecretsManagerClient): RSAPrivateKey {
        val secretValue = getSecretValue(secretsManagerClient, privateKeyName)
        return parsePrivateKey(secretValue)
    }

    @Bean
    fun fireblocksApiKey(secretsManagerClient: SecretsManagerClient): String = getSecretValue(secretsManagerClient, apiKeyName)

    @Bean
    fun fireblocksWebhookPublicKey(secretsManagerClient: SecretsManagerClient): RSAPublicKey {
        val secretValue = getSecretValue(secretsManagerClient, webhookPublicKeyName)
        return parsePublicKey(secretValue)
    }

    companion object {
        private val keyFactory = KeyFactory.getInstance("RSA")

        fun getSecretValue(
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

        fun parsePrivateKey(base64EncodedKey: String): RSAPrivateKey {
            val keyBytes = Base64.getDecoder().decode(base64EncodedKey)
            val keySpec = PKCS8EncodedKeySpec(keyBytes)
            return keyFactory.generatePrivate(keySpec) as RSAPrivateKey
        }

        fun parsePublicKey(base64EncodedKey: String): RSAPublicKey {
            val keyBytes = Base64.getDecoder().decode(base64EncodedKey)
            val keySpec = X509EncodedKeySpec(keyBytes)
            return keyFactory.generatePublic(keySpec) as RSAPublicKey
        }
    }
}
