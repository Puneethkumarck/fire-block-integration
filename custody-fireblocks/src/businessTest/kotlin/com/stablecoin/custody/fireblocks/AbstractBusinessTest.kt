package com.stablecoin.custody.fireblocks

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import com.stablecoin.custody.fireblocks.client.CustodyClientAutoConfiguration
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.Network
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.localstack.LocalStackContainer
import org.testcontainers.kafka.ConfluentKafkaContainer
import org.testcontainers.utility.DockerImageName
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient
import software.amazon.awssdk.services.secretsmanager.model.CreateSecretRequest
import java.security.KeyPairGenerator
import java.util.Base64

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@EnableAutoConfiguration(exclude = [CustodyClientAutoConfiguration::class])
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
abstract class AbstractBusinessTest {
    @Autowired
    protected lateinit var mockMvc: MockMvc

    companion object {
        private val network: Network = Network.newNetwork()

        @JvmStatic
        val postgres: PostgreSQLContainer<*> =
            PostgreSQLContainer("postgres:17-alpine")
                .withDatabaseName("custody_test")
                .withUsername("test")
                .withPassword("test")

        @JvmStatic
        val kafka: ConfluentKafkaContainer =
            ConfluentKafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.7.1"))
                .withNetwork(network)
                .withNetworkAliases("kafka")

        @JvmStatic
        val schemaRegistry: GenericContainer<*> =
            GenericContainer(DockerImageName.parse("confluentinc/cp-schema-registry:7.7.1"))
                .withExposedPorts(8081)
                .withNetwork(network)
                .withEnv("SCHEMA_REGISTRY_HOST_NAME", "schema-registry")
                .withEnv(
                    "SCHEMA_REGISTRY_KAFKASTORE_BOOTSTRAP_SERVERS",
                    "PLAINTEXT://kafka:9092",
                ).dependsOn(kafka)

        @JvmStatic
        val localstack: LocalStackContainer =
            LocalStackContainer(DockerImageName.parse("localstack/localstack:4.4.0"))
                .withServices(LocalStackContainer.Service.SECRETSMANAGER)

        @JvmStatic
        val wireMock: WireMockServer = WireMockServer(wireMockConfig().dynamicPort()).apply { start() }

        @JvmStatic
        val webhookKeyPair = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()

        init {
            postgres.start()
            kafka.start()
            schemaRegistry.start()
            localstack.start()
            seedSecrets()
        }

        private fun seedSecrets() {
            val client =
                SecretsManagerClient
                    .builder()
                    .endpointOverride(localstack.getEndpointOverride(LocalStackContainer.Service.SECRETSMANAGER))
                    .region(Region.of(localstack.region))
                    .credentialsProvider(
                        StaticCredentialsProvider.create(
                            AwsBasicCredentials.create(localstack.accessKey, localstack.secretKey),
                        ),
                    ).build()

            val privateKeyBase64 = Base64.getEncoder().encodeToString(webhookKeyPair.private.encoded)
            val publicKeyBase64 = Base64.getEncoder().encodeToString(webhookKeyPair.public.encoded)

            client.use {
                it.createSecret(
                    CreateSecretRequest
                        .builder()
                        .name("fireblocks/private-key")
                        .secretString(privateKeyBase64)
                        .build(),
                )
                it.createSecret(
                    CreateSecretRequest
                        .builder()
                        .name("fireblocks/api-key")
                        .secretString("test-api-key")
                        .build(),
                )
                it.createSecret(
                    CreateSecretRequest
                        .builder()
                        .name("fireblocks/webhook-public-key")
                        .secretString(publicKeyBase64)
                        .build(),
                )
            }
        }

        @JvmStatic
        @DynamicPropertySource
        fun configureProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", postgres::getJdbcUrl)
            registry.add("spring.datasource.username", postgres::getUsername)
            registry.add("spring.datasource.password", postgres::getPassword)
            registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers)
            registry.add("spring.kafka.properties.schema.registry.url") {
                "http://${schemaRegistry.host}:${schemaRegistry.getMappedPort(8081)}"
            }
            registry.add("aws.secretsmanager.endpoint") {
                localstack.getEndpointOverride(LocalStackContainer.Service.SECRETSMANAGER).toString()
            }
            registry.add("aws.secretsmanager.region") { localstack.region }
            registry.add("fireblocks.api.base-url") { "http://localhost:${wireMock.port()}" }
        }
    }
}
