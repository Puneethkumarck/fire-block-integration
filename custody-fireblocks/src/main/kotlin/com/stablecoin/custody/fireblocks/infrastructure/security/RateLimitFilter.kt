package com.stablecoin.custody.fireblocks.infrastructure.security

import com.github.benmanes.caffeine.cache.Caffeine
import com.stablecoin.custody.fireblocks.domain.shared.ErrorCode
import io.github.bucket4j.Bandwidth
import io.github.bucket4j.Bucket
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.util.concurrent.TimeUnit

@Component
class RateLimitFilter(
    private val rateLimitProperties: RateLimitProperties,
) : OncePerRequestFilter() {
    private val buckets =
        Caffeine
            .newBuilder()
            .maximumSize(100_000)
            .expireAfterAccess(1, TimeUnit.HOURS)
            .build<String, Bucket>()

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val clientId = resolveClientId()
        val bucket = buckets.get(clientId) { createBucket() }
        val probe = bucket.tryConsumeAndReturnRemaining(1)

        if (probe.isConsumed) {
            filterChain.doFilter(request, response)
        } else {
            val retryAfterSeconds = TimeUnit.NANOSECONDS.toSeconds(probe.nanosToWaitForRefill) + 1
            response.status = HttpStatus.TOO_MANY_REQUESTS.value()
            response.contentType = MediaType.APPLICATION_JSON_VALUE
            response.setHeader(HttpHeaders.RETRY_AFTER, retryAfterSeconds.toString())
            response.writer.write("""{"code":"${ErrorCode.RATE_LIMIT_EXCEEDED.code}","message":"Rate limit exceeded"}""")
        }
    }

    private fun resolveClientId(): String {
        val auth = SecurityContextHolder.getContext().authentication
        if (auth is JwtAuthenticationToken) {
            return auth.token.subject ?: "anonymous"
        }
        return "anonymous"
    }

    private fun createBucket(): Bucket =
        Bucket
            .builder()
            .addLimit(
                Bandwidth
                    .builder()
                    .capacity(rateLimitProperties.capacity)
                    .refillGreedy(rateLimitProperties.refillTokens, rateLimitProperties.refillDuration)
                    .build(),
            ).build()
}
