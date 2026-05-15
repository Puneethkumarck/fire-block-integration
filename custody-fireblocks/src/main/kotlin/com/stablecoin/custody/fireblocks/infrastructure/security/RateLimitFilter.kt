package com.stablecoin.custody.fireblocks.infrastructure.security

import com.github.benmanes.caffeine.cache.Caffeine
import io.github.bucket4j.Bandwidth
import io.github.bucket4j.Bucket
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
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

        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response)
        } else {
            response.status = HttpStatus.TOO_MANY_REQUESTS.value()
            response.contentType = MediaType.APPLICATION_JSON_VALUE
            response.writer.write("""{"code":"CUSTODY-4290","message":"Rate limit exceeded"}""")
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
