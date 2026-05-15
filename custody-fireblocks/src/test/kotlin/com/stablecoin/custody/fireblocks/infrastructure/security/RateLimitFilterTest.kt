package com.stablecoin.custody.fireblocks.infrastructure.security

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.mock.web.MockFilterChain
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import java.time.Duration
import java.time.Instant

class RateLimitFilterTest {
    private val properties = RateLimitProperties(capacity = 3, refillTokens = 3, refillDuration = Duration.ofMinutes(1))
    private val filter = RateLimitFilter(properties)

    @BeforeEach
    fun setUp() {
        SecurityContextHolder.clearContext()
    }

    @Test
    fun `should allow requests within rate limit`() {
        // given
        setJwtAuthentication("client-1")
        val request = MockHttpServletRequest()
        val response = MockHttpServletResponse()

        // when
        filter.doFilter(request, response, MockFilterChain())

        // then
        assertThat(response.status).isEqualTo(HttpStatus.OK.value())
    }

    @Test
    fun `should reject requests exceeding rate limit`() {
        // given
        setJwtAuthentication("client-2")

        repeat(3) {
            filter.doFilter(MockHttpServletRequest(), MockHttpServletResponse(), MockFilterChain())
        }

        val response = MockHttpServletResponse()

        // when
        filter.doFilter(MockHttpServletRequest(), response, MockFilterChain())

        // then
        assertThat(response.status).isEqualTo(HttpStatus.TOO_MANY_REQUESTS.value())
        assertThat(response.contentAsString).contains("CUSTODY-4002")
        assertThat(response.getHeader("Retry-After")).isNotNull()
    }

    @Test
    fun `should track rate limits per client`() {
        // given
        setJwtAuthentication("client-a")
        repeat(3) {
            filter.doFilter(MockHttpServletRequest(), MockHttpServletResponse(), MockFilterChain())
        }

        // when
        setJwtAuthentication("client-b")
        val response = MockHttpServletResponse()
        filter.doFilter(MockHttpServletRequest(), response, MockFilterChain())

        // then
        assertThat(response.status).isEqualTo(HttpStatus.OK.value())
    }

    @Test
    fun `should use anonymous bucket when no JWT present`() {
        // given
        val request = MockHttpServletRequest()
        val response = MockHttpServletResponse()

        // when
        filter.doFilter(request, response, MockFilterChain())

        // then
        assertThat(response.status).isEqualTo(HttpStatus.OK.value())
    }

    @Test
    fun `should fall back to jti when JWT subject is null`() {
        // given
        val jwt =
            Jwt
                .withTokenValue("mock-token")
                .header("alg", "RS256")
                .claim("jti", "unique-token-id")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build()
        SecurityContextHolder.getContext().authentication =
            JwtAuthenticationToken(jwt, listOf(SimpleGrantedAuthority("SCOPE_custody:read")))

        val response = MockHttpServletResponse()

        // when
        filter.doFilter(MockHttpServletRequest(), response, MockFilterChain())

        // then
        assertThat(response.status).isEqualTo(HttpStatus.OK.value())
    }

    @Test
    fun `should use anonymous bucket when JWT has no subject or jti`() {
        // given
        val jwt =
            Jwt
                .withTokenValue("mock-token")
                .header("alg", "RS256")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build()
        SecurityContextHolder.getContext().authentication =
            JwtAuthenticationToken(jwt, listOf(SimpleGrantedAuthority("SCOPE_custody:read")))

        val response = MockHttpServletResponse()

        // when
        filter.doFilter(MockHttpServletRequest(), response, MockFilterChain())

        // then
        assertThat(response.status).isEqualTo(HttpStatus.OK.value())
    }

    private fun setJwtAuthentication(subject: String) {
        val jwt =
            Jwt
                .withTokenValue("mock-token")
                .header("alg", "RS256")
                .subject(subject)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build()
        SecurityContextHolder.getContext().authentication =
            JwtAuthenticationToken(jwt, listOf(SimpleGrantedAuthority("SCOPE_custody:read")))
    }
}
