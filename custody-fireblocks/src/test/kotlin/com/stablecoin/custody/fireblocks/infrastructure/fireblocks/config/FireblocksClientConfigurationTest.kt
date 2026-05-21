package com.stablecoin.custody.fireblocks.infrastructure.fireblocks.config

import com.stablecoin.custody.fireblocks.infrastructure.fireblocks.client.FireblocksRateLimitException
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withStatus
import org.springframework.web.client.RestClient

class FireblocksClientConfigurationTest {
    private val meterRegistry = SimpleMeterRegistry()

    private fun createTestClient(): Pair<RestClient, MockRestServiceServer> {
        val restClientBuilder = RestClient.builder()
        val mockServer = MockRestServiceServer.bindTo(restClientBuilder).build()
        val rateLimitCounter =
            Counter
                .builder("fireblocks.api.rate_limited")
                .description("Fireblocks 429 rate limit responses")
                .register(meterRegistry)
        val restClient =
            restClientBuilder
                .defaultStatusHandler({ it.value() == 429 }) { _, response ->
                    handleRateLimit(rateLimitCounter, response)
                }.build()
        return restClient to mockServer
    }

    @Test
    fun `should throw FireblocksRateLimitException with retry after on 429 response`() {
        // given
        val (restClient, mockServer) = createTestClient()
        mockServer
            .expect(requestTo("/test"))
            .andRespond(
                withStatus(HttpStatus.TOO_MANY_REQUESTS)
                    .header("Retry-After", "5")
                    .body("rate limited"),
            )

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
        val (restClient, mockServer) = createTestClient()
        mockServer
            .expect(requestTo("/test"))
            .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS).body("rate limited"))

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
        val (restClient, mockServer) = createTestClient()
        mockServer
            .expect(requestTo("/test"))
            .andRespond(
                withStatus(HttpStatus.TOO_MANY_REQUESTS)
                    .header("Retry-After", "10")
                    .body("rate limited"),
            )

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
