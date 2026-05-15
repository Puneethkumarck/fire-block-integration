package com.stablecoin.custody.fireblocks.domain.audit

import com.stablecoin.custody.fireblocks.test.fixtures.anAuditLog
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class AuditLogTest {
    @Test
    fun `should create audit log with all fields`() {
        // given
        val details = mapOf("key" to "value", "count" to 42)

        // when
        val result =
            AuditLog.create(
                operation = AuditOperation.VAULT_CREATED,
                actor = "user-123",
                resourceId = "vault-456",
                status = AuditStatus.SUCCESS,
                fireblocksRequestId = "fb-req-789",
                details = details,
            )

        // then
        val expected =
            anAuditLog(
                actor = "user-123",
                operation = AuditOperation.VAULT_CREATED,
                resourceId = "vault-456",
                fireblocksRequestId = "fb-req-789",
                status = AuditStatus.SUCCESS,
                details = details,
            )
        assertThat(result)
            .usingRecursiveComparison()
            .ignoringFields("id", "timestamp")
            .isEqualTo(expected)
    }

    @Test
    fun `should create audit log with null details`() {
        // when
        val result =
            AuditLog.create(
                operation = AuditOperation.TRANSACTION_SUBMITTED,
                actor = "system",
                resourceId = "tx-123",
                status = AuditStatus.SUCCESS,
            )

        // then
        assertThat(result)
            .usingRecursiveComparison()
            .comparingOnlyFields("details")
            .isEqualTo(anAuditLog(details = null))
    }

    @Test
    fun `should create audit log with null fireblocksRequestId`() {
        // when
        val result =
            AuditLog.create(
                operation = AuditOperation.BALANCE_QUERIED,
                actor = "system",
                resourceId = "wallet-789",
                status = AuditStatus.SUCCESS,
            )

        // then
        assertThat(result)
            .usingRecursiveComparison()
            .comparingOnlyFields("fireblocksRequestId")
            .isEqualTo(anAuditLog(fireblocksRequestId = null))
    }

    @Test
    fun `should reject blank actor`() {
        // when/then
        assertThatThrownBy { anAuditLog(actor = " ") }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("actor must not be blank")
    }

    @Test
    fun `should reject blank resourceId`() {
        // when/then
        assertThatThrownBy { anAuditLog(resourceId = "") }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("resourceId must not be blank")
    }
}
