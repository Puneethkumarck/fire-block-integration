package com.stablecoin.custody.fireblocks.infrastructure.messaging

import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.boot.kafka.autoconfigure.KafkaProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.core.ProducerFactory
import org.springframework.kafka.support.serializer.JacksonJsonSerializer
import tools.jackson.databind.json.JsonMapper

@Configuration
class KafkaProducerConfiguration {
    @Bean
    fun producerFactory(
        kafkaProperties: KafkaProperties,
        jsonMapper: JsonMapper,
    ): ProducerFactory<String, Any> {
        val serializer = JacksonJsonSerializer<Any>(jsonMapper).noTypeInfo()
        return DefaultKafkaProducerFactory(kafkaProperties.buildProducerProperties(), StringSerializer(), serializer)
    }

    @Bean
    fun kafkaTemplate(producerFactory: ProducerFactory<String, Any>) = KafkaTemplate(producerFactory)
}
