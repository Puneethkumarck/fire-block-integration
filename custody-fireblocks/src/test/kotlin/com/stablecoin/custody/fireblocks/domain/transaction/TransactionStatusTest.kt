package com.stablecoin.custody.fireblocks.domain.transaction

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class TransactionStatusTest {
    @ParameterizedTest
    @CsvSource(
        "SUBMITTED, PROCESSING",
        "PENDING_SIGNATURE, PROCESSING",
        "PENDING_AUTHORIZATION, PROCESSING",
        "QUEUED, PROCESSING",
        "BROADCASTING, PROCESSING",
        "PENDING_AML_SCREENING, PROCESSING",
        "CANCELLING, PROCESSING",
        "CONFIRMING, CONFIRMING",
        "COMPLETED, CONFIRMED",
        "PARTIALLY_COMPLETED, FAILED",
        "FAILED, FAILED",
        "REJECTED, FAILED",
        "CANCELLED, FAILED",
        "BLOCKED, FAILED",
        "TIMEOUT, FAILED",
    )
    fun `should map Fireblocks status to internal status`(
        fireblocksStatus: String,
        expectedStatus: TransactionStatus,
    ) {
        // when
        val result = TransactionStatus.fromFireblocksStatus(fireblocksStatus)

        // then
        assertThat(result).isEqualTo(expectedStatus)
    }

    @ParameterizedTest
    @CsvSource(
        "UNKNOWN_STATUS",
        "INVALID",
        "PENDING",
    )
    fun `should throw on unknown Fireblocks status`(fireblocksStatus: String) {
        // when/then
        assertThatThrownBy { TransactionStatus.fromFireblocksStatus(fireblocksStatus) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("Unknown Fireblocks status: $fireblocksStatus")
    }
}
