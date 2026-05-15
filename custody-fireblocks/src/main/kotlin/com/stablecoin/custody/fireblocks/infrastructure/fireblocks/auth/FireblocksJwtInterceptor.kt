package com.stablecoin.custody.fireblocks.infrastructure.fireblocks.auth

import io.jsonwebtoken.Jwts
import org.springframework.http.HttpRequest
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.http.client.ClientHttpResponse
import org.springframework.stereotype.Component
import java.net.URI
import java.security.MessageDigest
import java.security.interfaces.RSAPrivateKey
import java.time.Instant
import java.util.Date
import java.util.UUID

@Component
class FireblocksJwtInterceptor(
    private val fireblocksPrivateKey: RSAPrivateKey,
    private val fireblocksApiKey: String,
) : ClientHttpRequestInterceptor {
    override fun intercept(
        request: HttpRequest,
        body: ByteArray,
        execution: ClientHttpRequestExecution,
    ): ClientHttpResponse {
        val jwt = buildJwt(request.uri, body)
        request.headers.setBearerAuth(jwt)
        request.headers.set("X-API-Key", fireblocksApiKey)
        return execution.execute(request, body)
    }

    private fun buildJwt(
        uri: URI,
        body: ByteArray,
    ): String {
        val now = Instant.now()
        val bodyHash = sha256Hex(body)
        val path = uri.rawPath + (uri.rawQuery?.let { "?$it" } ?: "")
        return Jwts
            .builder()
            .subject(fireblocksApiKey)
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plusSeconds(30)))
            .claim("nonce", UUID.randomUUID().toString())
            .claim("uri", path)
            .claim("bodyHash", bodyHash)
            .signWith(fireblocksPrivateKey, Jwts.SIG.RS256)
            .compact()
    }

    private fun sha256Hex(data: ByteArray): String =
        MessageDigest
            .getInstance("SHA-256")
            .digest(data)
            .joinToString("") { "%02x".format(it) }
}
