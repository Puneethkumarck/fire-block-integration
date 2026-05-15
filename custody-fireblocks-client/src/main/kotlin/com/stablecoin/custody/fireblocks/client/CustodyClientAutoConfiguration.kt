package com.stablecoin.custody.fireblocks.client

import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.web.client.RestClient
import org.springframework.web.client.support.RestClientAdapter
import org.springframework.web.service.invoker.HttpServiceProxyFactory

@AutoConfiguration
class CustodyClientAutoConfiguration {
    @Bean
    fun custodyClient(restClientBuilder: RestClient.Builder): CustodyClient {
        val restClient = restClientBuilder.build()
        val factory =
            HttpServiceProxyFactory
                .builderFor(RestClientAdapter.create(restClient))
                .build()
        return factory.createClient(CustodyClient::class.java)
    }
}
