package com.stablecoin.custody.fireblocks.infrastructure.security

import com.stablecoin.custody.fireblocks.domain.shared.logger
import jakarta.servlet.FilterChain
import jakarta.servlet.ReadListener
import jakarta.servlet.ServletInputStream
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletRequestWrapper
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import tools.jackson.databind.ObjectMapper
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.InputStreamReader
import java.security.Signature
import java.security.interfaces.RSAPublicKey
import java.time.Duration
import java.time.Instant
import java.util.Base64

private val log = logger<FireblocksWebhookAuthenticationFilter>()

@Component
class FireblocksWebhookAuthenticationFilter(
    private val fireblocksWebhookPublicKey: RSAPublicKey,
    private val objectMapper: ObjectMapper,
) : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val bodyBytes = request.inputStream.readAllBytes()
        val cachedRequest = CachedBodyRequestWrapper(request, bodyBytes)

        val signatureHeader = request.getHeader(SIGNATURE_HEADER)
        if (signatureHeader == null) {
            log.warn("Missing {} header", SIGNATURE_HEADER)
            writeErrorResponse(response)
            return
        }

        if (!verifySignature(bodyBytes, signatureHeader)) {
            log.warn("Invalid webhook signature")
            writeErrorResponse(response)
            return
        }

        if (!isWithinReplayWindow(bodyBytes)) {
            log.warn("Webhook timestamp outside replay window")
            writeErrorResponse(response)
            return
        }

        val authentication =
            UsernamePasswordAuthenticationToken(
                "fireblocks-webhook",
                null,
                listOf(SimpleGrantedAuthority("ROLE_WEBHOOK")),
            )
        SecurityContextHolder.getContext().authentication = authentication

        filterChain.doFilter(cachedRequest, response)
    }

    private fun verifySignature(
        bodyBytes: ByteArray,
        signatureBase64: String,
    ): Boolean =
        try {
            val signatureBytes = Base64.getDecoder().decode(signatureBase64)
            val sig = Signature.getInstance("SHA512withRSA")
            sig.initVerify(fireblocksWebhookPublicKey)
            sig.update(bodyBytes)
            sig.verify(signatureBytes)
        } catch (e: Exception) {
            log.warn("Signature verification failed: {}", e.message)
            false
        }

    private fun isWithinReplayWindow(bodyBytes: ByteArray): Boolean =
        try {
            val tree = objectMapper.readTree(bodyBytes)
            val createdAt = tree.get("createdAt")?.asLong() ?: return false
            val eventTime = Instant.ofEpochMilli(createdAt)
            val age = Duration.between(eventTime, Instant.now())
            age <= REPLAY_WINDOW
        } catch (e: Exception) {
            log.warn("Failed to parse webhook timestamp: {}", e.message)
            false
        }

    private fun writeErrorResponse(response: HttpServletResponse) {
        response.status = HttpStatus.UNAUTHORIZED.value()
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.writer.write("""{"code":"CUSTODY-4001","message":"Webhook verification failed"}""")
    }

    companion object {
        private const val SIGNATURE_HEADER = "Fireblocks-Signature"
        private val REPLAY_WINDOW = Duration.ofMinutes(5)
    }
}

private class CachedBodyRequestWrapper(
    request: HttpServletRequest,
    private val cachedBody: ByteArray,
) : HttpServletRequestWrapper(request) {
    override fun getInputStream(): ServletInputStream {
        val byteArrayInputStream = ByteArrayInputStream(cachedBody)
        return object : ServletInputStream() {
            override fun read(): Int = byteArrayInputStream.read()

            override fun isFinished(): Boolean = byteArrayInputStream.available() == 0

            override fun isReady(): Boolean = true

            override fun setReadListener(listener: ReadListener?): Unit = throw UnsupportedOperationException()
        }
    }

    override fun getReader(): BufferedReader = BufferedReader(InputStreamReader(inputStream))
}
