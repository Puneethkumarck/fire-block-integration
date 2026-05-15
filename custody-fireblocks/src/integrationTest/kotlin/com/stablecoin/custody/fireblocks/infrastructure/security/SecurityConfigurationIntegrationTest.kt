package com.stablecoin.custody.fireblocks.infrastructure.security

import com.stablecoin.custody.fireblocks.AbstractIntegrationTest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.security.Signature
import java.security.interfaces.RSAPrivateKey
import java.time.Instant
import java.util.Base64

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class SecurityConfigurationIntegrationTest : AbstractIntegrationTest() {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    fun `should reject unauthenticated GET to api endpoint`() {
        // when
        // then
        mockMvc
            .perform(get("/api/v1/vaults"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `should accept GET with custody read scope`() {
        // when
        // then
        mockMvc
            .perform(
                get("/api/v1/vaults")
                    .with(jwt().authorities(SimpleGrantedAuthority("SCOPE_custody:read"))),
            ).andExpect(status().isNotFound)
    }

    @Test
    fun `should reject GET with custody write scope only`() {
        // when
        // then
        mockMvc
            .perform(
                get("/api/v1/vaults")
                    .with(jwt().authorities(SimpleGrantedAuthority("SCOPE_custody:write"))),
            ).andExpect(status().isForbidden)
    }

    @Test
    fun `should reject POST with custody read scope only`() {
        // when
        // then
        mockMvc
            .perform(
                post("/api/v1/vaults")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}")
                    .with(jwt().authorities(SimpleGrantedAuthority("SCOPE_custody:read"))),
            ).andExpect(status().isForbidden)
    }

    @Test
    fun `should accept POST with custody write scope`() {
        // when
        // then
        mockMvc
            .perform(
                post("/api/v1/vaults")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}")
                    .with(jwt().authorities(SimpleGrantedAuthority("SCOPE_custody:write"))),
            ).andExpect(status().isNotFound)
    }

    @Test
    fun `should accept webhook with valid signature`() {
        // given
        val body = """{"type":"TRANSACTION_STATUS_UPDATED","createdAt":${Instant.now().toEpochMilli()}}"""
        val signature = signWithWebhookKey(body)

        // when
        // then
        mockMvc
            .perform(
                post("/api/v1/webhooks/fireblocks")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body)
                    .header("Fireblocks-Signature", signature),
            ).andExpect(status().isNotFound)
    }

    @Test
    fun `should reject webhook with invalid signature`() {
        // given
        val body = """{"type":"TRANSACTION_STATUS_UPDATED","createdAt":${Instant.now().toEpochMilli()}}"""
        val invalidSignature = Base64.getEncoder().encodeToString("invalid".toByteArray())

        // when
        // then
        mockMvc
            .perform(
                post("/api/v1/webhooks/fireblocks")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body)
                    .header("Fireblocks-Signature", invalidSignature),
            ).andExpect(status().isUnauthorized)
    }

    @Test
    fun `should reject webhook with replayed timestamp`() {
        // given
        val oldTimestamp = Instant.now().minusSeconds(301).toEpochMilli()
        val body = """{"type":"TRANSACTION_STATUS_UPDATED","createdAt":$oldTimestamp}"""
        val signature = signWithWebhookKey(body)

        // when
        // then
        mockMvc
            .perform(
                post("/api/v1/webhooks/fireblocks")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body)
                    .header("Fireblocks-Signature", signature),
            ).andExpect(status().isUnauthorized)
    }

    @Test
    fun `should rate limit API requests after capacity exceeded`() {
        // given — unique subject so this test gets its own rate limit bucket
        val jwt =
            jwt()
                .jwt { it.subject("rate-limit-test-client") }
                .authorities(SimpleGrantedAuthority("SCOPE_custody:read"))

        // when — exhaust the rate limit (capacity=3 in test profile)
        repeat(3) {
            mockMvc
                .perform(get("/api/v1/vaults").with(jwt))
                .andExpect(status().isNotFound)
        }

        // then — next request should be rate limited
        mockMvc
            .perform(get("/api/v1/vaults").with(jwt))
            .andExpect(status().isTooManyRequests)
    }

    @Test
    fun `should not rate limit webhook requests`() {
        // given
        repeat(5) { i ->
            val body = """{"type":"TRANSACTION_STATUS_UPDATED","createdAt":${Instant.now().toEpochMilli() + i}}"""
            val signature = signWithWebhookKey(body)

            // when
            // then — webhook should never be rate limited
            mockMvc
                .perform(
                    post("/api/v1/webhooks/fireblocks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .header("Fireblocks-Signature", signature),
                ).andExpect(status().isNotFound)
        }
    }

    @Test
    fun `should not return CORS headers`() {
        // when
        // then
        mockMvc
            .perform(
                get("/api/v1/vaults")
                    .header("Origin", "https://evil.com")
                    .with(jwt().authorities(SimpleGrantedAuthority("SCOPE_custody:read"))),
            ).andExpect(header().doesNotExist("Access-Control-Allow-Origin"))
    }

    private fun signWithWebhookKey(body: String): String {
        val sig = Signature.getInstance("SHA512withRSA")
        sig.initSign(webhookKeyPair.private as RSAPrivateKey)
        sig.update(body.toByteArray())
        return Base64.getEncoder().encodeToString(sig.sign())
    }
}
