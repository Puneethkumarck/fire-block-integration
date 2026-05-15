package com.stablecoin.custody.fireblocks.infrastructure.secrets

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient
import java.net.URI

@Configuration
class SecretsManagerClientConfiguration(
    @param:Value("\${aws.secretsmanager.endpoint:}") private val endpoint: String,
    @param:Value("\${aws.secretsmanager.region:us-east-1}") private val region: String,
) {
    @Bean
    fun secretsManagerClient(): SecretsManagerClient {
        val builder =
            SecretsManagerClient
                .builder()
                .region(Region.of(region))

        if (endpoint.isNotBlank()) {
            builder.endpointOverride(URI.create(endpoint))
            builder.credentialsProvider(
                StaticCredentialsProvider.create(
                    AwsBasicCredentials.create("localstack", "localstack"),
                ),
            )
        }

        return builder.build()
    }
}
