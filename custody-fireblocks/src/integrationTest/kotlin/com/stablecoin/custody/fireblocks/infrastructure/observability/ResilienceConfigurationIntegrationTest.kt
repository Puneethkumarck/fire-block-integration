package com.stablecoin.custody.fireblocks.infrastructure.observability

import com.stablecoin.custody.fireblocks.AbstractIntegrationTest
import io.github.resilience4j.bulkhead.BulkheadRegistry
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import io.github.resilience4j.retry.RetryRegistry
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class ResilienceConfigurationIntegrationTest : AbstractIntegrationTest() {
    @Autowired
    private lateinit var circuitBreakerRegistry: CircuitBreakerRegistry

    @Autowired
    private lateinit var bulkheadRegistry: BulkheadRegistry

    @Autowired
    private lateinit var retryRegistry: RetryRegistry

    @Test
    fun `should configure fireblocks circuit breaker`() {
        // when
        val circuitBreaker = circuitBreakerRegistry.circuitBreaker("fireblocks")

        // then
        val config = circuitBreaker.circuitBreakerConfig
        assertThat(config.failureRateThreshold).isEqualTo(50f)
        assertThat(config.slidingWindowSize).isEqualTo(10)
        assertThat(config.permittedNumberOfCallsInHalfOpenState).isEqualTo(3)
    }

    @Test
    fun `should configure fireblocks-balance circuit breaker`() {
        // when
        val circuitBreaker = circuitBreakerRegistry.circuitBreaker("fireblocks-balance")

        // then
        val config = circuitBreaker.circuitBreakerConfig
        assertThat(config.failureRateThreshold).isEqualTo(50f)
        assertThat(config.slidingWindowSize).isEqualTo(10)
    }

    @Test
    fun `should configure fireblocks bulkhead`() {
        // when
        val bulkhead = bulkheadRegistry.bulkhead("fireblocks")

        // then
        assertThat(bulkhead.bulkheadConfig.maxConcurrentCalls).isEqualTo(25)
    }

    @Test
    fun `should configure fireblocks retry`() {
        // when
        val retry = retryRegistry.retry("fireblocks")

        // then
        assertThat(retry.retryConfig.maxAttempts).isEqualTo(3)
    }
}
