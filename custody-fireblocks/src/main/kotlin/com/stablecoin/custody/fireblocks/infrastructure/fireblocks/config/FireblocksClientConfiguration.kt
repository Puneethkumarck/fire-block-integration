package com.stablecoin.custody.fireblocks.infrastructure.fireblocks.config

import com.stablecoin.custody.fireblocks.infrastructure.fireblocks.auth.FireblocksJwtInterceptor
import com.stablecoin.custody.fireblocks.infrastructure.fireblocks.client.FireblocksRateLimitException
import com.stablecoin.custody.fireblocks.infrastructure.fireblocks.client.FireblocksTransactionClient
import com.stablecoin.custody.fireblocks.infrastructure.fireblocks.client.FireblocksVaultClient
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType
import org.springframework.http.client.ClientHttpResponse
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestClient
import org.springframework.web.client.support.RestClientAdapter
import org.springframework.web.service.invoker.HttpServiceProxyFactory

internal fun handleRateLimit(
    rateLimitCounter: Counter,
    response: ClientHttpResponse,
) {
    rateLimitCounter.increment()
    val retryAfter = response.headers.getFirst("Retry-After")?.toLongOrNull()
    throw FireblocksRateLimitException(
        retryAfterSeconds = retryAfter,
        cause =
            HttpClientErrorException.create(
                response.statusCode,
                response.statusText,
                response.headers,
                response.body.readAllBytes(),
                null,
            ),
    )
}

@Configuration
class FireblocksClientConfiguration(
    private val properties: FireblocksProperties,
) {
    @Bean
    fun fireblocksRestClient(
        jwtInterceptor: FireblocksJwtInterceptor,
        meterRegistry: MeterRegistry,
    ): RestClient {
        val factory =
            SimpleClientHttpRequestFactory().apply {
                setConnectTimeout(properties.api.connectTimeout)
                setReadTimeout(properties.api.readTimeout)
            }
        val rateLimitCounter =
            Counter
                .builder("fireblocks.api.rate_limited")
                .description("Fireblocks 429 rate limit responses")
                .register(meterRegistry)
        return RestClient
            .builder()
            .baseUrl(properties.api.baseUrl)
            .requestFactory(factory)
            .requestInterceptor(jwtInterceptor)
            .defaultHeaders { headers -> headers.contentType = MediaType.APPLICATION_JSON }
            .defaultStatusHandler({ it.value() == 429 }) { _, response ->
                handleRateLimit(rateLimitCounter, response)
            }.build()
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
