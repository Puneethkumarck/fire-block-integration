package com.stablecoin.custody.fireblocks.infrastructure.fireblocks.config

import com.stablecoin.custody.fireblocks.infrastructure.fireblocks.auth.FireblocksJwtInterceptor
import com.stablecoin.custody.fireblocks.infrastructure.fireblocks.client.FireblocksTransactionClient
import com.stablecoin.custody.fireblocks.infrastructure.fireblocks.client.FireblocksVaultClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.web.client.RestClient
import org.springframework.web.client.support.RestClientAdapter
import org.springframework.web.service.invoker.HttpServiceProxyFactory

@Configuration
class FireblocksClientConfiguration(
    private val properties: FireblocksProperties,
) {
    @Bean
    fun fireblocksRestClient(jwtInterceptor: FireblocksJwtInterceptor): RestClient {
        val factory =
            SimpleClientHttpRequestFactory().apply {
                setConnectTimeout(properties.api.connectTimeout)
                setReadTimeout(properties.api.readTimeout)
            }
        return RestClient
            .builder()
            .baseUrl(properties.api.baseUrl)
            .requestFactory(factory)
            .requestInterceptor(jwtInterceptor)
            .defaultHeaders { headers -> headers.contentType = MediaType.APPLICATION_JSON }
            .build()
    }

    @Bean
    fun fireblocksHttpServiceProxyFactory(fireblocksRestClient: RestClient): HttpServiceProxyFactory =
        HttpServiceProxyFactory
            .builderFor(RestClientAdapter.create(fireblocksRestClient))
            .build()

    @Bean
    fun fireblocksVaultClient(fireblocksHttpServiceProxyFactory: HttpServiceProxyFactory): FireblocksVaultClient =
        fireblocksHttpServiceProxyFactory.createClient(FireblocksVaultClient::class.java)

    @Bean
    fun fireblocksTransactionClient(fireblocksHttpServiceProxyFactory: HttpServiceProxyFactory): FireblocksTransactionClient =
        fireblocksHttpServiceProxyFactory.createClient(FireblocksTransactionClient::class.java)
}
