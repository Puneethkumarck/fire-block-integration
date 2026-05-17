package com.stablecoin.custody.fireblocks.application.exception

import com.stablecoin.custody.fireblocks.AbstractMockMvcIntegrationTest
import com.stablecoin.custody.fireblocks.domain.exception.VaultNotFoundException
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import org.hamcrest.Matchers.hasKey
import org.junit.jupiter.api.Test
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@Import(ExceptionTestController::class)
class GlobalExceptionHandlerIntegrationTest : AbstractMockMvcIntegrationTest() {
    private fun uniqueJwt() = jwt().jwt { it.subject(UUID.randomUUID().toString()) }

    @Test
    fun `should return structured error for invalid request body`() {
        mockMvc
            .perform(
                post("/api/test/validate")
                    .with(uniqueJwt())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"name": ""}"""),
            ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("CUSTODY-0001"))
            .andExpect(jsonPath("$.status").value("Bad Request"))
            .andExpect(jsonPath("$.message").value("Validation failed"))
            .andExpect(jsonPath("$.details.name").value("must not be blank"))
    }

    @Test
    fun `should return structured error for missing required field`() {
        mockMvc
            .perform(
                post("/api/test/validate")
                    .with(uniqueJwt())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"name": "  "}"""),
            ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("CUSTODY-0001"))
            .andExpect(jsonPath("$.details.name").exists())
    }

    @Test
    fun `should return 404 for non-existent vault`() {
        mockMvc
            .perform(
                get("/api/test/vault-not-found")
                    .with(uniqueJwt()),
            ).andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("CUSTODY-1001"))
            .andExpect(jsonPath("$.status").value("Not Found"))
            .andExpect(jsonPath("$.message").value("Vault not found for customerRefId: cust-999"))
    }

    @Test
    fun `should return 500 for unexpected errors`() {
        mockMvc
            .perform(
                get("/api/test/unexpected")
                    .with(uniqueJwt()),
            ).andExpect(status().isInternalServerError)
            .andExpect(jsonPath("$.code").value("CUSTODY-5001"))
            .andExpect(jsonPath("$.status").value("Internal Server Error"))
            .andExpect(jsonPath("$.message").value("An unexpected error occurred"))
            .andExpect(jsonPath("$.stackTrace").doesNotExist())
    }

    @Test
    fun `should include traceId field in error response`() {
        mockMvc
            .perform(
                get("/api/test/vault-not-found")
                    .with(uniqueJwt()),
            ).andExpect(status().isNotFound)
            .andExpect(jsonPath("$", hasKey("traceId")))
    }
}

@Validated
@RestController
@RequestMapping("/api/test")
class ExceptionTestController {
    @GetMapping("/vault-not-found")
    fun vaultNotFound(): Nothing = throw VaultNotFoundException("cust-999")

    @GetMapping("/unexpected")
    fun unexpected(): Nothing = throw RuntimeException("Boom")

    @PostMapping("/validate")
    fun validate(
        @RequestBody @Valid request: IntegrationTestRequest,
    ): String = "ok"
}

data class IntegrationTestRequest(
    @field:NotBlank
    val name: String = "",
)
