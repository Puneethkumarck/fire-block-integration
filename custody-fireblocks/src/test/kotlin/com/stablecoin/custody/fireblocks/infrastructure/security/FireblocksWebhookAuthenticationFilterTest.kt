package com.stablecoin.custody.fireblocks.infrastructure.security

import io.mockk.mockk
import jakarta.servlet.FilterChain
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.core.context.SecurityContextHolder
import tools.jackson.databind.ObjectMapper
import java.security.KeyPairGenerator
import java.security.Signature
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.time.Instant
import java.util.Base64

class FireblocksWebhookAuthenticationFilterTest {
    private val keyPair = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
    private val publicKey = keyPair.public as RSAPublicKey
    private val privateKey = keyPair.private as RSAPrivateKey
    private val objectMapper = ObjectMapper()
    private val filter = FireblocksWebhookAuthenticationFilter(publicKey, objectMapper)
    private val filterChain = mockk<FilterChain>(relaxed = true)

    @BeforeEach
    fun setUp() {
        SecurityContextHolder.clearContext()
    }

    @Test
    fun `should accept valid signature and recent timestamp`() {
        // given
        val body = """{"type":"TRANSACTION_STATUS_UPDATED","createdAt":${Instant.now().toEpochMilli()}}"""
        val request = createRequest(body, sign(body))
        val response = MockHttpServletResponse()

        // when
        filter.doFilter(request, response, filterChain)

        // then
        assertThat(response.status).isEqualTo(HttpStatus.OK.value())
        assertThat(SecurityContextHolder.getContext().authentication).isNotNull()
        assertThat(SecurityContextHolder.getContext().authentication!!.name).isEqualTo("fireblocks-webhook")
    }

    @Test
    fun `should reject missing Fireblocks-Signature header`() {
        // given
        val body = """{"type":"TRANSACTION_STATUS_UPDATED","createdAt":${Instant.now().toEpochMilli()}}"""
        val request =
            MockHttpServletRequest().apply {
                setContent(body.toByteArray())
                contentType = "application/json"
            }
        val response = MockHttpServletResponse()

        // when
        filter.doFilter(request, response, filterChain)

        // then
        assertThat(response.status).isEqualTo(HttpStatus.UNAUTHORIZED.value())
        assertThat(response.contentAsString).contains("CUSTODY-4001")
    }

    @Test
    fun `should reject invalid signature`() {
        // given
        val body = """{"type":"TRANSACTION_STATUS_UPDATED","createdAt":${Instant.now().toEpochMilli()}}"""
        val invalidSignature = Base64.getEncoder().encodeToString("invalid-signature".toByteArray())
        val request = createRequest(body, invalidSignature)
        val response = MockHttpServletResponse()

        // when
        filter.doFilter(request, response, filterChain)

        // then
        assertThat(response.status).isEqualTo(HttpStatus.UNAUTHORIZED.value())
        assertThat(response.contentAsString).contains("CUSTODY-4001")
    }

    @Test
    fun `should reject replayed webhook with old timestamp`() {
        // given
        val oldTimestamp = Instant.now().minusSeconds(301).toEpochMilli()
        val body = """{"type":"TRANSACTION_STATUS_UPDATED","createdAt":$oldTimestamp}"""
        val request = createRequest(body, sign(body))
        val response = MockHttpServletResponse()

        // when
        filter.doFilter(request, response, filterChain)

        // then
        assertThat(response.status).isEqualTo(HttpStatus.UNAUTHORIZED.value())
        assertThat(response.contentAsString).contains("CUSTODY-4001")
    }

    @Test
    fun `should accept webhook near 5 minute boundary`() {
        // given
        val boundaryTimestamp = Instant.now().minusSeconds(295).toEpochMilli()
        val body = """{"type":"TRANSACTION_STATUS_UPDATED","createdAt":$boundaryTimestamp}"""
        val request = createRequest(body, sign(body))
        val response = MockHttpServletResponse()

        // when
        filter.doFilter(request, response, filterChain)

        // then
        assertThat(response.status).isEqualTo(HttpStatus.OK.value())
        assertThat(SecurityContextHolder.getContext().authentication).isNotNull()
    }

    @Test
    fun `should cache request body for downstream controller`() {
        // given
        val body = """{"type":"TRANSACTION_STATUS_UPDATED","createdAt":${Instant.now().toEpochMilli()}}"""
        val request = createRequest(body, sign(body))
        val response = MockHttpServletResponse()

        // when
        filter.doFilter(request, response, filterChain)

        // then
        assertThat(response.status).isEqualTo(HttpStatus.OK.value())
        io.mockk.verify {
            filterChain.doFilter(
                match { it is jakarta.servlet.http.HttpServletRequestWrapper },
                response,
            )
        }
    }

    @Test
    fun `should reject webhook with future timestamp beyond replay window`() {
        // given
        val futureTimestamp = Instant.now().plusSeconds(301).toEpochMilli()
        val body = """{"type":"TRANSACTION_STATUS_UPDATED","createdAt":$futureTimestamp}"""
        val request = createRequest(body, sign(body))
        val response = MockHttpServletResponse()

        // when
        filter.doFilter(request, response, filterChain)

        // then
        assertThat(response.status).isEqualTo(HttpStatus.UNAUTHORIZED.value())
        assertThat(response.contentAsString).contains("CUSTODY-4001")
    }

    @Test
    fun `should reject webhook with missing createdAt field`() {
        // given
        val body = """{"type":"TRANSACTION_STATUS_UPDATED"}"""
        val request = createRequest(body, sign(body))
        val response = MockHttpServletResponse()

        // when
        filter.doFilter(request, response, filterChain)

        // then
        assertThat(response.status).isEqualTo(HttpStatus.UNAUTHORIZED.value())
        assertThat(response.contentAsString).contains("CUSTODY-4001")
    }

    @Test
    fun `should reject webhook with non-JSON body`() {
        // given
        val body = "not-json-content"
        val request = createRequest(body, sign(body))
        val response = MockHttpServletResponse()

        // when
        filter.doFilter(request, response, filterChain)

        // then
        assertThat(response.status).isEqualTo(HttpStatus.UNAUTHORIZED.value())
        assertThat(response.contentAsString).contains("CUSTODY-4001")
    }

    @Test
    fun `should return 401 with CUSTODY-4001 error code on failure`() {
        // given
        val body = """{"type":"TRANSACTION_STATUS_UPDATED","createdAt":${Instant.now().toEpochMilli()}}"""
        val request = createRequest(body, "not-valid-base64!!")
        val response = MockHttpServletResponse()

        // when
        filter.doFilter(request, response, filterChain)

        // then
        assertThat(response.status).isEqualTo(HttpStatus.UNAUTHORIZED.value())
        assertThat(response.contentType).isEqualTo("application/json")
        assertThat(response.contentAsString).contains("CUSTODY-4001")
        assertThat(response.contentAsString).contains("Webhook verification failed")
    }

    private fun createRequest(
        body: String,
        signature: String,
    ): MockHttpServletRequest =
        MockHttpServletRequest().apply {
            setContent(body.toByteArray())
            contentType = "application/json"
            addHeader("Fireblocks-Signature", signature)
        }

    private fun sign(body: String): String {
        val sig = Signature.getInstance("SHA512withRSA")
        sig.initSign(privateKey)
        sig.update(body.toByteArray())
        return Base64.getEncoder().encodeToString(sig.sign())
    }
}
