package com.stablecoin.custody.fireblocks.domain.allocation

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class AllocationStatusTest {
    @ParameterizedTest
    @CsvSource(
        "PENDING, LOCKED",
        "PENDING, FAILED",
        "LOCKED, CONSUMED",
        "LOCKED, RELEASED",
        "CONSUMED, RELEASED",
    )
    fun `should allow valid transitions`(
        from: AllocationStatus,
        to: AllocationStatus,
    ) {
        // when
        val result = AllocationStatus.canTransition(from, to)

        // then
        assertThat(result).isTrue()
    }

    @ParameterizedTest
    @CsvSource(
        "PENDING, CONSUMED",
        "PENDING, RELEASED",
        "LOCKED, FAILED",
        "LOCKED, PENDING",
        "CONSUMED, LOCKED",
        "CONSUMED, FAILED",
        "CONSUMED, PENDING",
        "RELEASED, LOCKED",
        "RELEASED, CONSUMED",
        "RELEASED, PENDING",
        "RELEASED, FAILED",
        "FAILED, LOCKED",
        "FAILED, CONSUMED",
        "FAILED, RELEASED",
        "FAILED, PENDING",
    )
    fun `should reject invalid transitions`(
        from: AllocationStatus,
        to: AllocationStatus,
    ) {
        // when
        val result = AllocationStatus.canTransition(from, to)

        // then
        assertThat(result).isFalse()
    }

    @Test
    fun `should mark RELEASED and FAILED as terminal`() {
        // then
        assertThat(AllocationStatus.RELEASED.terminal).isTrue()
        assertThat(AllocationStatus.FAILED.terminal).isTrue()
    }

    @Test
    fun `should mark PENDING LOCKED and CONSUMED as non-terminal`() {
        // then
        assertThat(AllocationStatus.PENDING.terminal).isFalse()
        assertThat(AllocationStatus.LOCKED.terminal).isFalse()
        assertThat(AllocationStatus.CONSUMED.terminal).isFalse()
    }
}
