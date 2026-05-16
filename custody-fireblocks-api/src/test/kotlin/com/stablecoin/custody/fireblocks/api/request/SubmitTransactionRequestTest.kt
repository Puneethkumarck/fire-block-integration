package com.stablecoin.custody.fireblocks.api.request

import com.stablecoin.custody.fireblocks.api.test.fixtures.aSubmitTransactionRequest
import jakarta.validation.Validation
import jakarta.validation.Validator
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class SubmitTransactionRequestTest {
    private val validator: Validator = Validation.buildDefaultValidatorFactory().use { it.validator }

    @Test
    fun `should reject blank currency`() {
        // given
        val request = aSubmitTransactionRequest().copy(currency = "")

        // when
        val violations = validator.validate(request)

        // then
        assertThat(violations).anyMatch { it.propertyPath.toString() == "currency" }
    }

    @Test
    fun `should reject currency longer than 10 chars`() {
        // given
        val request = aSubmitTransactionRequest().copy(currency = "a".repeat(11))

        // when
        val violations = validator.validate(request)

        // then
        assertThat(violations).anyMatch { it.propertyPath.toString() == "currency" }
    }

    @Test
    fun `should reject blank protocol`() {
        // given
        val request = aSubmitTransactionRequest().copy(protocol = "")

        // when
        val violations = validator.validate(request)

        // then
        assertThat(violations).anyMatch { it.propertyPath.toString() == "protocol" }
    }

    @Test
    fun `should reject protocol longer than 20 chars`() {
        // given
        val request = aSubmitTransactionRequest().copy(protocol = "a".repeat(21))

        // when
        val violations = validator.validate(request)

        // then
        assertThat(violations).anyMatch { it.propertyPath.toString() == "protocol" }
    }

    @Test
    fun `should reject zero amount`() {
        // given
        val request = aSubmitTransactionRequest().copy(amount = BigDecimal.ZERO)

        // when
        val violations = validator.validate(request)

        // then
        assertThat(violations).anyMatch { it.propertyPath.toString() == "amount" }
    }

    @Test
    fun `should reject negative amount`() {
        // given
        val request = aSubmitTransactionRequest().copy(amount = BigDecimal("-1"))

        // when
        val violations = validator.validate(request)

        // then
        assertThat(violations).anyMatch { it.propertyPath.toString() == "amount" }
    }

    @Test
    fun `should reject blank destinationAddress`() {
        // given
        val request = aSubmitTransactionRequest().copy(destinationAddress = "")

        // when
        val violations = validator.validate(request)

        // then
        assertThat(violations).anyMatch { it.propertyPath.toString() == "destinationAddress" }
    }

    @Test
    fun `should reject destinationAddress longer than 256 chars`() {
        // given
        val request = aSubmitTransactionRequest().copy(destinationAddress = "a".repeat(257))

        // when
        val violations = validator.validate(request)

        // then
        assertThat(violations).anyMatch { it.propertyPath.toString() == "destinationAddress" }
    }

    @Test
    fun `should reject blank externalTxId`() {
        // given
        val request = aSubmitTransactionRequest().copy(externalTxId = "")

        // when
        val violations = validator.validate(request)

        // then
        assertThat(violations).anyMatch { it.propertyPath.toString() == "externalTxId" }
    }

    @Test
    fun `should reject blank sourceVaultId`() {
        // given
        val request = aSubmitTransactionRequest().copy(sourceVaultId = "")

        // when
        val violations = validator.validate(request)

        // then
        assertThat(violations).anyMatch { it.propertyPath.toString() == "sourceVaultId" }
    }

    @Test
    fun `should accept valid request with optional fields null`() {
        // given
        val request = aSubmitTransactionRequest()

        // when
        val violations = validator.validate(request)

        // then
        assertThat(violations).isEmpty()
    }
}
