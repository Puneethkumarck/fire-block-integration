package com.stablecoin.custody.fireblocks.application.exception

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import com.stablecoin.custody.fireblocks.domain.exception.FireblocksApiException
import com.stablecoin.custody.fireblocks.domain.exception.FireblocksTimeoutException
import com.stablecoin.custody.fireblocks.domain.exception.InvalidTransactionStateException
import com.stablecoin.custody.fireblocks.domain.exception.TransactionDuplicateException
import com.stablecoin.custody.fireblocks.domain.exception.VaultNotActiveException
import com.stablecoin.custody.fireblocks.domain.exception.VaultNotFoundException
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matchers.nullValue
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

class GlobalExceptionHandlerTest {
    private lateinit var mockMvc: MockMvc

    @BeforeEach
    fun setUp() {
        mockMvc =
            MockMvcBuilders
                .standaloneSetup(TestController())
                .setControllerAdvice(GlobalExceptionHandler())
                .build()
    }

    @AfterEach
    fun tearDown() {
        MDC.clear()
    }

    @Test
    fun `should map VaultNotFoundException to 404 with CUSTODY-1001`() {
        mockMvc
            .perform(get("/test/vault-not-found"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("CUSTODY-1001"))
            .andExpect(jsonPath("$.status").value("Not Found"))
            .andExpect(jsonPath("$.message").value("Vault not found for customerRefId: cust-001"))
    }

    @Test
    fun `should map TransactionDuplicateException to 409 with CUSTODY-1005`() {
        mockMvc
            .perform(get("/test/transaction-duplicate"))
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.code").value("CUSTODY-1005"))
            .andExpect(jsonPath("$.status").value("Conflict"))
            .andExpect(jsonPath("$.message").value("Transaction already exists with externalTxId: ext-tx-001"))
    }

    @Test
    fun `should map VaultNotActiveException to 422 with CUSTODY-1006`() {
        mockMvc
            .perform(get("/test/vault-not-active"))
            .andExpect(status().is4xxClientError)
            .andExpect(jsonPath("$.code").value("CUSTODY-1006"))
            .andExpect(jsonPath("$.status").value("Unprocessable Content"))
            .andExpect(jsonPath("$.message").value("Vault vault-001 is not active"))
    }

    @Test
    fun `should map InvalidTransactionStateException to 422 with CUSTODY-2001`() {
        mockMvc
            .perform(get("/test/invalid-transaction-state"))
            .andExpect(status().is4xxClientError)
            .andExpect(jsonPath("$.code").value("CUSTODY-2001"))
            .andExpect(jsonPath("$.status").value("Unprocessable Content"))
            .andExpect(jsonPath("$.message").value("Transaction tx-001 cannot transition from CREATED to CONFIRMED"))
    }

    @Test
    fun `should map FireblocksApiException to 502 with CUSTODY-3001`() {
        mockMvc
            .perform(get("/test/fireblocks-api-error"))
            .andExpect(status().isBadGateway)
            .andExpect(jsonPath("$.code").value("CUSTODY-3001"))
            .andExpect(jsonPath("$.status").value("Bad Gateway"))
            .andExpect(jsonPath("$.message").value("Fireblocks API unavailable"))
    }

    @Test
    fun `should map FireblocksTimeoutException to 504 with CUSTODY-3002`() {
        mockMvc
            .perform(get("/test/fireblocks-timeout"))
            .andExpect(status().isGatewayTimeout)
            .andExpect(jsonPath("$.code").value("CUSTODY-3002"))
            .andExpect(jsonPath("$.status").value("Gateway Timeout"))
            .andExpect(jsonPath("$.message").value("Fireblocks request timed out"))
    }

    @Test
    fun `should map validation error to 400 with CUSTODY-0001 and field details`() {
        mockMvc
            .perform(
                post("/test/validate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"name": ""}"""),
            ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("CUSTODY-0001"))
            .andExpect(jsonPath("$.status").value("Bad Request"))
            .andExpect(jsonPath("$.message").value("Validation failed"))
            .andExpect(jsonPath("$.details.name").value("must not be blank"))
    }

    @Test
    fun `should map DataIntegrityViolationException to 409 with CUSTODY-1002`() {
        mockMvc
            .perform(get("/test/data-integrity"))
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.code").value("CUSTODY-1002"))
            .andExpect(jsonPath("$.status").value("Conflict"))
            .andExpect(jsonPath("$.message").value("Resource already exists"))
    }

    @Test
    fun `should map OptimisticLockingFailureException to 409 with CUSTODY-5002`() {
        mockMvc
            .perform(get("/test/optimistic-lock"))
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.code").value("CUSTODY-5002"))
            .andExpect(jsonPath("$.status").value("Conflict"))
            .andExpect(jsonPath("$.message").value("Resource was modified concurrently, please retry"))
    }

    @Test
    fun `should map unexpected exception to 500 with CUSTODY-5001`() {
        mockMvc
            .perform(get("/test/unexpected"))
            .andExpect(status().isInternalServerError)
            .andExpect(jsonPath("$.code").value("CUSTODY-5001"))
            .andExpect(jsonPath("$.status").value("Internal Server Error"))
            .andExpect(jsonPath("$.message").value("An unexpected error occurred"))
    }

    @Test
    fun `should include traceId from MDC`() {
        MDC.put("traceId", "abc-123-trace")
        mockMvc
            .perform(get("/test/vault-not-found"))
            .andExpect(jsonPath("$.traceId").value("abc-123-trace"))
    }

    @Test
    fun `should return null traceId when MDC has no traceId`() {
        mockMvc
            .perform(get("/test/vault-not-found"))
            .andExpect(jsonPath("$.traceId").value(nullValue()))
    }

    @Test
    fun `should not include stack trace in any response`() {
        mockMvc
            .perform(get("/test/unexpected"))
            .andExpect(jsonPath("$.stackTrace").doesNotExist())
            .andExpect(jsonPath("$.cause").doesNotExist())
            .andExpect(jsonPath("$.suppressed").doesNotExist())
    }

    @Test
    fun `should log 4xx at WARN level`() {
        val logAppender = captureHandlerLogs()

        mockMvc
            .perform(get("/test/vault-not-found"))
            .andExpect(status().isNotFound)

        assertThat(logAppender.list)
            .anyMatch { it.level == Level.WARN && it.formattedMessage.contains("Client error") }
        detachAppender(logAppender)
    }

    @Test
    fun `should log 5xx at ERROR level`() {
        val logAppender = captureHandlerLogs()

        mockMvc
            .perform(get("/test/fireblocks-api-error"))
            .andExpect(status().isBadGateway)

        assertThat(logAppender.list)
            .anyMatch { it.level == Level.ERROR && it.formattedMessage.contains("Server error") }
        detachAppender(logAppender)
    }

    private fun captureHandlerLogs(): ListAppender<ILoggingEvent> {
        val appender = ListAppender<ILoggingEvent>()
        appender.start()
        handlerLogger().addAppender(appender)
        return appender
    }

    private fun detachAppender(appender: ListAppender<ILoggingEvent>) {
        handlerLogger().detachAppender(appender)
    }

    private fun handlerLogger() = LoggerFactory.getLogger(GlobalExceptionHandler::class.java) as ch.qos.logback.classic.Logger
}

@Validated
@RestController
private class TestController {
    @GetMapping("/test/vault-not-found")
    fun vaultNotFound(): Nothing = throw VaultNotFoundException("cust-001")

    @GetMapping("/test/transaction-duplicate")
    fun transactionDuplicate(): Nothing = throw TransactionDuplicateException("ext-tx-001")

    @GetMapping("/test/vault-not-active")
    fun vaultNotActive(): Nothing = throw VaultNotActiveException("vault-001")

    @GetMapping("/test/invalid-transaction-state")
    fun invalidTransactionState(): Nothing = throw InvalidTransactionStateException("tx-001", "CREATED", "CONFIRMED")

    @GetMapping("/test/fireblocks-api-error")
    fun fireblocksApiError(): Nothing = throw FireblocksApiException("Fireblocks API unavailable")

    @GetMapping("/test/fireblocks-timeout")
    fun fireblocksTimeout(): Nothing = throw FireblocksTimeoutException("Fireblocks request timed out")

    @PostMapping("/test/validate")
    fun validate(
        @RequestBody @Valid request: TestRequest,
    ): String = "ok"

    @GetMapping("/test/data-integrity")
    fun dataIntegrity(): Nothing = throw DataIntegrityViolationException("Duplicate key")

    @GetMapping("/test/optimistic-lock")
    fun optimisticLock(): Nothing = throw OptimisticLockingFailureException("Row was updated")

    @GetMapping("/test/unexpected")
    fun unexpected(): Nothing = throw RuntimeException("Something went wrong")
}

private data class TestRequest(
    @field:NotBlank
    val name: String,
)
