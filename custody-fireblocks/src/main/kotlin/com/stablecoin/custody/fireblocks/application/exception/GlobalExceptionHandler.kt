package com.stablecoin.custody.fireblocks.application.exception

import com.stablecoin.custody.fireblocks.api.error.ApiError
import com.stablecoin.custody.fireblocks.domain.shared.CustodyException
import com.stablecoin.custody.fireblocks.domain.shared.logger
import org.slf4j.MDC
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.client.RestClientException
import org.springframework.web.context.request.WebRequest
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler
import java.io.IOException
import java.net.SocketTimeoutException

private val log = logger<GlobalExceptionHandler>()

@RestControllerAdvice
class GlobalExceptionHandler : ResponseEntityExceptionHandler() {
    @ExceptionHandler(CustodyException::class)
    fun handleCustodyException(
        ex: CustodyException,
        request: WebRequest,
    ): ResponseEntity<Any> {
        val status = HttpStatusCode.valueOf(ex.errorCode.httpStatus)
        if (ex.errorCode.httpStatus in 400..499) {
            log.warn("Client error: code={}, message={}", ex.errorCode.code, ex.message)
        } else {
            log.error("Server error: code={}", ex.errorCode.code, ex)
        }
        return ResponseEntity.status(status).body(
            ApiError(
                code = ex.errorCode.code,
                status = statusReasonPhrase(ex.errorCode.httpStatus),
                message = ex.message ?: "An error occurred",
                traceId = MDC.get("traceId"),
            ),
        )
    }

    override fun handleMethodArgumentNotValid(
        ex: MethodArgumentNotValidException,
        headers: HttpHeaders,
        status: HttpStatusCode,
        request: WebRequest,
    ): ResponseEntity<Any>? {
        val errors = ex.bindingResult.fieldErrors.associate { it.field to it.defaultMessage }
        log.warn("Validation error: {}", errors)
        return ResponseEntity.badRequest().body(
            ApiError(
                code = "CUSTODY-0001",
                status = "Bad Request",
                message = "Validation failed",
                traceId = MDC.get("traceId"),
                details = errors,
            ),
        )
    }

    @ExceptionHandler(DataIntegrityViolationException::class)
    fun handleDataIntegrity(ex: DataIntegrityViolationException): ResponseEntity<Any> {
        log.warn("Data integrity violation: {}", ex.message)
        return ResponseEntity.status(409).body(
            ApiError(
                code = "CUSTODY-1002",
                status = "Conflict",
                message = "Resource already exists",
                traceId = MDC.get("traceId"),
            ),
        )
    }

    @ExceptionHandler(OptimisticLockingFailureException::class)
    fun handleOptimisticLock(ex: OptimisticLockingFailureException): ResponseEntity<Any> {
        log.warn("Optimistic locking failure: {}", ex.message)
        return ResponseEntity.status(409).body(
            ApiError(
                code = "CUSTODY-5002",
                status = "Conflict",
                message = "Resource was modified concurrently, please retry",
                traceId = MDC.get("traceId"),
            ),
        )
    }

    @ExceptionHandler(RestClientException::class)
    fun handleUpstreamError(
        ex: RestClientException,
        request: WebRequest,
    ): ResponseEntity<Any> {
        val rootCause = ex.rootCause ?: ex.cause
        return when {
            rootCause is SocketTimeoutException -> {
                log.error("Upstream API timeout", ex)
                ResponseEntity.status(504).body(
                    ApiError(
                        code = "CUSTODY-3002",
                        status = "Gateway Timeout",
                        message = "Upstream service timed out",
                        traceId = MDC.get("traceId"),
                    ),
                )
            }
            rootCause is IOException -> {
                log.error("Upstream API access error", ex)
                ResponseEntity.status(502).body(
                    ApiError(
                        code = "CUSTODY-3001",
                        status = "Bad Gateway",
                        message = "Upstream service unavailable",
                        traceId = MDC.get("traceId"),
                    ),
                )
            }
            else -> {
                log.error("Upstream API error", ex)
                ResponseEntity.status(502).body(
                    ApiError(
                        code = "CUSTODY-3001",
                        status = "Bad Gateway",
                        message = "Upstream service error",
                        traceId = MDC.get("traceId"),
                    ),
                )
            }
        }
    }

    @ExceptionHandler(Exception::class)
    fun handleUnexpected(
        ex: Exception,
        request: WebRequest,
    ): ResponseEntity<Any> {
        log.error("Unexpected error", ex)
        return ResponseEntity.internalServerError().body(
            ApiError(
                code = "CUSTODY-5001",
                status = "Internal Server Error",
                message = "An unexpected error occurred",
                traceId = MDC.get("traceId"),
            ),
        )
    }

    private fun statusReasonPhrase(httpStatus: Int): String =
        HttpStatus
            .valueOf(httpStatus)
            .reasonPhrase
}
