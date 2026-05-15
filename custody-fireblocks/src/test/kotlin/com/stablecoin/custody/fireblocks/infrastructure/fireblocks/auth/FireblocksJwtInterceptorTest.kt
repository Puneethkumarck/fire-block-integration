package com.stablecoin.custody.fireblocks.infrastructure.fireblocks.auth

import io.jsonwebtoken.Jwts
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpResponse
import org.springframework.mock.http.client.MockClientHttpRequest
import java.net.URI
import java.security.KeyPairGenerator
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey

class FireblocksJwtInterceptorTest {
    private val keyPair = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
    private val privateKey = keyPair.private as RSAPrivateKey
    private val publicKey = keyPair.public as RSAPublicKey
    private val apiKey = "test-api-key-123"
    private val interceptor = FireblocksJwtInterceptor(privateKey, apiKey)

    @Test
    fun `should set Authorization Bearer header with valid JWT`() {
        // given
        val request = MockClientHttpRequest(HttpMethod.POST, URI.create("https://api.fireblocks.io/v1/vault/accounts"))
        val body = """{"name":"test"}""".toByteArray()
        val execution = mockExecution()

        // when
        interceptor.intercept(request, body, execution)

        // then
        val authHeader = request.headers.getFirst(HttpHeaders.AUTHORIZATION)
        assertThat(authHeader).startsWith("Bearer ")
        val jwt = authHeader!!.removePrefix("Bearer ")
        val claims =
            Jwts
                .parser()
                .verifyWith(publicKey)
                .build()
                .parseSignedClaims(jwt)
        assertThat(claims.payload.subject).isEqualTo(apiKey)
    }

    @Test
    fun `should set X-API-Key header`() {
        // given
        val request = MockClientHttpRequest(HttpMethod.GET, URI.create("https://api.fireblocks.io/v1/vault/accounts/1"))
        val execution = mockExecution()

        // when
        interceptor.intercept(request, ByteArray(0), execution)

        // then
        assertThat(request.headers.getFirst("X-API-Key")).isEqualTo(apiKey)
    }

    @Test
    fun `should include URI path in JWT claims`() {
        // given
        val request = MockClientHttpRequest(HttpMethod.GET, URI.create("https://api.fireblocks.io/v1/vault/accounts/42"))
        val execution = mockExecution()

        // when
        interceptor.intercept(request, ByteArray(0), execution)

        // then
        val claims = parseClaims(request)
        assertThat(claims["uri"]).isEqualTo("/v1/vault/accounts/42")
    }

    @Test
    fun `should include SHA-256 body hash in JWT claims`() {
        // given
        val request = MockClientHttpRequest(HttpMethod.POST, URI.create("https://api.fireblocks.io/v1/transactions"))
        val body = """{"amount":"1.5"}""".toByteArray()
        val execution = mockExecution()

        // when
        interceptor.intercept(request, body, execution)

        // then
        val claims = parseClaims(request)
        val bodyHash = claims["bodyHash"] as String
        assertThat(bodyHash).matches("[a-f0-9]{64}")
    }

    @Test
    fun `should set expiration 30 seconds from issuedAt`() {
        // given
        val request = MockClientHttpRequest(HttpMethod.GET, URI.create("https://api.fireblocks.io/v1/vault/accounts/1"))
        val execution = mockExecution()

        // when
        interceptor.intercept(request, ByteArray(0), execution)

        // then
        val jwt = extractJwt(request)
        val claims =
            Jwts
                .parser()
                .verifyWith(publicKey)
                .build()
                .parseSignedClaims(jwt)
                .payload
        val diffSeconds = (claims.expiration.time - claims.issuedAt.time) / 1000
        assertThat(diffSeconds).isEqualTo(30)
    }

    @Test
    fun `should include nonce as UUID`() {
        // given
        val request = MockClientHttpRequest(HttpMethod.GET, URI.create("https://api.fireblocks.io/v1/vault/accounts/1"))
        val execution = mockExecution()

        // when
        interceptor.intercept(request, ByteArray(0), execution)

        // then
        val claims = parseClaims(request)
        val nonce = claims["nonce"] as String
        assertThat(nonce).matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")
    }

    @Test
    fun `should sign with RS256 algorithm`() {
        // given
        val request = MockClientHttpRequest(HttpMethod.GET, URI.create("https://api.fireblocks.io/v1/vault/accounts/1"))
        val execution = mockExecution()

        // when
        interceptor.intercept(request, ByteArray(0), execution)

        // then
        val jwt = extractJwt(request)
        val header =
            Jwts
                .parser()
                .verifyWith(publicKey)
                .build()
                .parseSignedClaims(jwt)
                .header
        assertThat(header.algorithm).isEqualTo("RS256")
    }

    @Test
    fun `should handle empty body`() {
        // given
        val request = MockClientHttpRequest(HttpMethod.GET, URI.create("https://api.fireblocks.io/v1/vault/accounts/1"))
        val execution = mockExecution()

        // when
        interceptor.intercept(request, ByteArray(0), execution)

        // then
        val claims = parseClaims(request)
        val bodyHash = claims["bodyHash"] as String
        assertThat(bodyHash).isEqualTo("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855")
    }

    @Test
    fun `should include query string in URI claim`() {
        // given
        val request =
            MockClientHttpRequest(
                HttpMethod.GET,
                URI.create("https://api.fireblocks.io/v1/vault/accounts?status=ACTIVE&limit=10"),
            )
        val execution = mockExecution()

        // when
        interceptor.intercept(request, ByteArray(0), execution)

        // then
        val claims = parseClaims(request)
        assertThat(claims["uri"]).isEqualTo("/v1/vault/accounts?status=ACTIVE&limit=10")
    }

    private fun mockExecution(): ClientHttpRequestExecution {
        val execution = mockk<ClientHttpRequestExecution>()
        val response = mockk<ClientHttpResponse>()
        val bodySlot = slot<ByteArray>()
        every { execution.execute(any(), capture(bodySlot)) } returns response
        return execution
    }

    private fun extractJwt(request: MockClientHttpRequest): String =
        request.headers.getFirst(HttpHeaders.AUTHORIZATION)!!.removePrefix("Bearer ")

    private fun parseClaims(request: MockClientHttpRequest): Map<String, Any> {
        val jwt = extractJwt(request)
        return Jwts
            .parser()
            .verifyWith(publicKey)
            .build()
            .parseSignedClaims(jwt)
            .payload
    }
}
