package com.stablecoin.custody.fireblocks.api.request

import jakarta.validation.Validation
import jakarta.validation.Validator
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class SubmitTransactionRequestTest {
    private val validator: Validator = Validation.buildDefaultValidatorFactory().validator

    @Test
    fun `should reject blank currency`() {
        // given
        val request = aValidRequest().copy(currency = "")

        // when
        val violations = validator.validate(request)

        // then
        assertThat(violations).anyMatch { it.propertyPath.toString() == "currency" }
    }

    @Test
    fun `should reject currency longer than 10 chars`() {
        // given
        val request = aValidRequest().copy(currency = "a".repeat(11))

        // when
        val violations = validator.validate(request)

        // then
        assertThat(violations).anyMatch { it.propertyPath.toString() == "currency" }
    }

    @Test
    fun `should reject blank protocol`() {
        // given
        val request = aValidRequest().copy(protocol = "")

        // when
        val violations = validator.validate(request)

        // then
        assertThat(violations).anyMatch { it.propertyPath.toString() == "protocol" }
    }

    @Test
    fun `should reject protocol longer than 20 chars`() {
        // given
        val request = aValidRequest().copy(protocol = "a".repeat(21))

        // when
        val violations = validator.validate(request)

        // then
        assertThat(violations).anyMatch { it.propertyPath.toString() == "protocol" }
    }

    @Test
    fun `should reject zero amount`() {
        // given
        val request = aValidRequest().copy(amount = BigDecimal.ZERO)

        // when
        val violations = validator.validate(request)

        // then
        assertThat(violations).anyMatch { it.propertyPath.toString() == "amount" }
    }

    @Test
    fun `should reject negative amount`() {
        // given
        val request = aValidRequest().copy(amount = BigDecimal("-1"))

        // when
        val violations = validator.validate(request)

        // then
        assertThat(violations).anyMatch { it.propertyPath.toString() == "amount" }
    }

    @Test
    fun `should reject blank destinationAddress`() {
        // given
        val request = aValidRequest().copy(destinationAddress = "")

        // when
        val violations = validator.validate(request)

        // then
        assertThat(violations).anyMatch { it.propertyPath.toString() == "destinationAddress" }
    }

    @Test
    fun `should reject destinationAddress longer than 256 chars`() {
        // given
        val request = aValidRequest().copy(destinationAddress = "a".repeat(257))

        // when
        val violations = validator.validate(request)

        // then
        assertThat(violations).anyMatch { it.propertyPath.toString() == "destinationAddress" }
    }

    @Test
    fun `should reject blank externalTxId`() {
        // given
        val request = aValidRequest().copy(externalTxId = "")

        // when
        val violations = validator.validate(request)

        // then
        assertThat(violations).anyMatch { it.propertyPath.toString() == "externalTxId" }
    }

    @Test
    fun `should reject blank sourceVaultId`() {
        // given
        val request = aValidRequest().copy(sourceVaultId = "")

        // when
        val violations = validator.validate(request)

        // then
        assertThat(violations).anyMatch { it.propertyPath.toString() == "sourceVaultId" }
    }

    @Test
    fun `should accept valid request with optional fields null`() {
        // given
        val request = aValidRequest()

        // when
        val violations = validator.validate(request)

        // then
        assertThat(violations).isEmpty()
    }

    private fun aValidRequest() =
        SubmitTransactionRequest(
            externalTxId = "ext-tx-001",
            sourceVaultId = "vault-001",
            destinationAddress = "0x1234567890abcdef1234567890abcdef12345678",
            currency = "EURC",
            protocol = "ETH",
            amount = BigDecimal("100.00"),
        )
}
