package com.stablecoin.custody.fireblocks.api.request

import com.stablecoin.custody.fireblocks.api.test.fixtures.anActivateAssetRequest
import jakarta.validation.Validation
import jakarta.validation.Validator
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ActivateAssetRequestTest {
    private val validator: Validator = Validation.buildDefaultValidatorFactory().use { it.validator }

    @Test
    fun `should reject blank currency`() {
        // given
        val request = anActivateAssetRequest().copy(currency = "")

        // when
        val violations = validator.validate(request)

        // then
        assertThat(violations).anyMatch { it.propertyPath.toString() == "currency" }
    }

    @Test
    fun `should reject currency longer than 10 chars`() {
        // given
        val request = anActivateAssetRequest().copy(currency = "a".repeat(11))

        // when
        val violations = validator.validate(request)

        // then
        assertThat(violations).anyMatch { it.propertyPath.toString() == "currency" }
    }

    @Test
    fun `should reject blank protocol`() {
        // given
        val request = anActivateAssetRequest().copy(protocol = "")

        // when
        val violations = validator.validate(request)

        // then
        assertThat(violations).anyMatch { it.propertyPath.toString() == "protocol" }
    }

    @Test
    fun `should reject protocol longer than 20 chars`() {
        // given
        val request = anActivateAssetRequest().copy(protocol = "a".repeat(21))

        // when
        val violations = validator.validate(request)

        // then
        assertThat(violations).anyMatch { it.propertyPath.toString() == "protocol" }
    }

    @Test
    fun `should accept valid request`() {
        // given
        val request = anActivateAssetRequest()

        // when
        val violations = validator.validate(request)

        // then
        assertThat(violations).isEmpty()
    }
}
