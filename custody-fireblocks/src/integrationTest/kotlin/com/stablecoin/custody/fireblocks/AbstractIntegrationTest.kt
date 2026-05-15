package com.stablecoin.custody.fireblocks

import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.Network
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.localstack.LocalStackContainer
import org.testcontainers.kafka.ConfluentKafkaContainer
import org.testcontainers.utility.DockerImageName

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
abstract class AbstractIntegrationTest {
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

        init {
            postgres.start()
            kafka.start()
            schemaRegistry.start()
            localstack.start()
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
            registry.add("spring.cloud.aws.secretsmanager.endpoint") {
                localstack.getEndpointOverride(LocalStackContainer.Service.SECRETSMANAGER).toString()
            }
            registry.add("spring.cloud.aws.region.static") { localstack.region }
        }
    }
}
