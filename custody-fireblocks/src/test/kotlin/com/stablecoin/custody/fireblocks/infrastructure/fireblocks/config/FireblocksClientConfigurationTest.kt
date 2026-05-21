package com.stablecoin.custody.fireblocks.infrastructure.fireblocks.config

import com.stablecoin.custody.fireblocks.infrastructure.fireblocks.client.FireblocksRateLimitException
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withStatus
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestClient

class FireblocksClientConfigurationTest {
    private val meterRegistry = SimpleMeterRegistry()

    @Test
    fun `should throw FireblocksRateLimitException with retry after on 429 response`() {
        // given
        val restClientBuilder = RestClient.builder()
        val mockServer = MockRestServiceServer.bindTo(restClientBuilder).build()

        mockServer
            .expect(requestTo("/test"))
            .andRespond(
                withStatus(HttpStatus.TOO_MANY_REQUESTS)
                    .header("Retry-After", "5")
                    .body("rate limited"),
            )

        val rateLimitCounter = meterRegistry.counter("fireblocks.api.rate_limited")
        val restClient =
            restClientBuilder
                .defaultStatusHandler({ it.value() == 429 }) { _, response ->
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
                }.build()

        // when / then
        assertThatThrownBy {
            restClient
                .get()
                .uri("/test")
                .retrieve()
                .body(String::class.java)
        }.isInstanceOf(FireblocksRateLimitException::class.java)
            .satisfies({ ex ->
                assertThat((ex as FireblocksRateLimitException).retryAfterSeconds).isEqualTo(5L)
            })
    }

    @Test
    fun `should parse null retry after when header is missing`() {
        // given
        val restClientBuilder = RestClient.builder()
        val mockServer = MockRestServiceServer.bindTo(restClientBuilder).build()

        mockServer
            .expect(requestTo("/test"))
            .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS).body("rate limited"))

        val rateLimitCounter = meterRegistry.counter("fireblocks.api.rate_limited")
        val restClient =
            restClientBuilder
                .defaultStatusHandler({ it.value() == 429 }) { _, response ->
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
                }.build()

        // when / then
        assertThatThrownBy {
            restClient
                .get()
                .uri("/test")
                .retrieve()
                .body(String::class.java)
        }.isInstanceOf(FireblocksRateLimitException::class.java)
            .satisfies({ ex ->
                assertThat((ex as FireblocksRateLimitException).retryAfterSeconds).isNull()
            })
    }

    @Test
    fun `should increment rate limit counter on 429`() {
        // given
        val restClientBuilder = RestClient.builder()
        val mockServer = MockRestServiceServer.bindTo(restClientBuilder).build()

        mockServer
            .expect(requestTo("/test"))
            .andRespond(
                withStatus(HttpStatus.TOO_MANY_REQUESTS)
                    .header("Retry-After", "10")
                    .body("rate limited"),
            )

        val rateLimitCounter = meterRegistry.counter("fireblocks.api.rate_limited")
        val restClient =
            restClientBuilder
                .defaultStatusHandler({ it.value() == 429 }) { _, response ->
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
                }.build()

        // when
        try {
            restClient
                .get()
                .uri("/test")
                .retrieve()
                .body(String::class.java)
        } catch (_: FireblocksRateLimitException) {
        }

        // then
        assertThat(meterRegistry.counter("fireblocks.api.rate_limited").count()).isEqualTo(1.0)
    }
}
