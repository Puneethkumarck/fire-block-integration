package com.stablecoin.custody.fireblocks.infrastructure.messaging

import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.boot.kafka.autoconfigure.KafkaProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.core.ProducerFactory
import org.springframework.kafka.support.serializer.JacksonJsonSerializer

@Configuration
class KafkaProducerConfiguration {
    @Bean
    fun producerFactory(kafkaProperties: KafkaProperties): ProducerFactory<String, Any> =
        DefaultKafkaProducerFactory(
            kafkaProperties.buildProducerProperties(),
            StringSerializer(),
            JacksonJsonSerializer<Any>().noTypeInfo(),
        )

    @Bean
    fun kafkaTemplate(producerFactory: ProducerFactory<String, Any>) = KafkaTemplate(producerFactory)
}
