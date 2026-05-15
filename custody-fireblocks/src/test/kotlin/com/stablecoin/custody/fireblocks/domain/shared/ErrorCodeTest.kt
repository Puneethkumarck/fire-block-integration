package com.stablecoin.custody.fireblocks.domain.shared

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class ErrorCodeTest {
    @ParameterizedTest
    @CsvSource(
        "VALIDATION_FAILED, CUSTODY-0001, 400",
        "VAULT_NOT_FOUND, CUSTODY-1001, 404",
        "VAULT_ALREADY_EXISTS, CUSTODY-1002, 409",
        "ASSET_NOT_FOUND, CUSTODY-1003, 404",
        "TRANSACTION_NOT_FOUND, CUSTODY-1004, 404",
        "TRANSACTION_DUPLICATE, CUSTODY-1005, 409",
        "VAULT_NOT_ACTIVE, CUSTODY-1006, 422",
        "INVALID_TRANSACTION_STATE, CUSTODY-2001, 422",
        "INSUFFICIENT_BALANCE, CUSTODY-2002, 422",
        "FIREBLOCKS_API_ERROR, CUSTODY-3001, 502",
        "FIREBLOCKS_TIMEOUT, CUSTODY-3002, 504",
        "WEBHOOK_VERIFICATION_FAILED, CUSTODY-4001, 401",
        "INTERNAL_ERROR, CUSTODY-5001, 500",
        "CONCURRENT_MODIFICATION, CUSTODY-5002, 409",
    )
    fun `should map each error code to correct HTTP status`(
        name: String,
        expectedCode: String,
        expectedHttpStatus: Int,
    ) {
        // given
        val errorCode = ErrorCode.valueOf(name)

        // when/then
        assertThat(errorCode.code).isEqualTo(expectedCode)
        assertThat(errorCode.httpStatus).isEqualTo(expectedHttpStatus)
    }

    @Test
    fun `should have unique error codes`() {
        // given
        val codes = ErrorCode.entries.map { it.code }

        // when/then
        assertThat(codes).doesNotHaveDuplicates()
    }
}
