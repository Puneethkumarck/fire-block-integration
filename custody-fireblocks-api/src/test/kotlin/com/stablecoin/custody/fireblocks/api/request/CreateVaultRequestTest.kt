package com.stablecoin.custody.fireblocks.api.request

import jakarta.validation.Validation
import jakarta.validation.Validator
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class CreateVaultRequestTest {
    private val validator: Validator = Validation.buildDefaultValidatorFactory().use { it.validator }

    @Test
    fun `should reject blank customerRefId`() {
        // given
        val request = CreateVaultRequest(customerRefId = "", name = "Test Vault")

        // when
        val violations = validator.validate(request)

        // then
        assertThat(violations).anyMatch { it.propertyPath.toString() == "customerRefId" }
    }

    @Test
    fun `should reject customerRefId longer than 128 chars`() {
        // given
        val request = CreateVaultRequest(customerRefId = "a".repeat(129), name = "Test Vault")

        // when
        val violations = validator.validate(request)

        // then
        assertThat(violations).anyMatch { it.propertyPath.toString() == "customerRefId" }
    }

    @Test
    fun `should reject blank name`() {
        // given
        val request = CreateVaultRequest(customerRefId = "ref-001", name = "")

        // when
        val violations = validator.validate(request)

        // then
        assertThat(violations).anyMatch { it.propertyPath.toString() == "name" }
    }

    @Test
    fun `should reject name longer than 128 chars`() {
        // given
        val request = CreateVaultRequest(customerRefId = "ref-001", name = "a".repeat(129))

        // when
        val violations = validator.validate(request)

        // then
        assertThat(violations).anyMatch { it.propertyPath.toString() == "name" }
    }

    @Test
    fun `should accept valid request`() {
        // given
        val request = CreateVaultRequest(customerRefId = "ref-001", name = "My Vault")

        // when
        val violations = validator.validate(request)

        // then
        assertThat(violations).isEmpty()
    }
}
