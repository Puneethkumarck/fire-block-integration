package com.stablecoin.custody.fireblocks.domain.shared

import com.stablecoin.custody.fireblocks.domain.exception.AssetNotFoundException
import com.stablecoin.custody.fireblocks.domain.exception.FireblocksApiException
import com.stablecoin.custody.fireblocks.domain.exception.FireblocksTimeoutException
import com.stablecoin.custody.fireblocks.domain.exception.InsufficientBalanceException
import com.stablecoin.custody.fireblocks.domain.exception.InvalidTransactionStateException
import com.stablecoin.custody.fireblocks.domain.exception.TransactionDuplicateException
import com.stablecoin.custody.fireblocks.domain.exception.TransactionNotFoundException
import com.stablecoin.custody.fireblocks.domain.exception.VaultAlreadyExistsException
import com.stablecoin.custody.fireblocks.domain.exception.VaultNotActiveException
import com.stablecoin.custody.fireblocks.domain.exception.VaultNotFoundException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.net.SocketTimeoutException

class CustodyExceptionTest {
    @Test
    fun `should create VaultNotFoundException with correct code and message`() {
        // when
        val exception = VaultNotFoundException("cust-ref-123")

        // then
        assertThat(exception.errorCode).isEqualTo(ErrorCode.VAULT_NOT_FOUND)
        assertThat(exception.message).isEqualTo("Vault not found for customerRefId: cust-ref-123")
    }

    @Test
    fun `should create VaultAlreadyExistsException with correct code and message`() {
        // when
        val exception = VaultAlreadyExistsException("cust-ref-456")

        // then
        assertThat(exception.errorCode).isEqualTo(ErrorCode.VAULT_ALREADY_EXISTS)
        assertThat(exception.message).isEqualTo("Vault already exists for customerRefId: cust-ref-456")
    }

    @Test
    fun `should create AssetNotFoundException with correct code and message`() {
        // when
        val exception = AssetNotFoundException("vault-1", "ETH")

        // then
        assertThat(exception.errorCode).isEqualTo(ErrorCode.ASSET_NOT_FOUND)
        assertThat(exception.message).isEqualTo("Asset ETH not found in vault vault-1")
    }

    @Test
    fun `should create TransactionNotFoundException with correct code and message`() {
        // when
        val exception = TransactionNotFoundException("ext-tx-789")

        // then
        assertThat(exception.errorCode).isEqualTo(ErrorCode.TRANSACTION_NOT_FOUND)
        assertThat(exception.message).isEqualTo("Transaction not found for externalTxId: ext-tx-789")
    }

    @Test
    fun `should create TransactionDuplicateException with correct code`() {
        // when
        val exception = TransactionDuplicateException("ext-tx-456")

        // then
        assertThat(exception.errorCode).isEqualTo(ErrorCode.TRANSACTION_DUPLICATE)
        assertThat(exception.message).isEqualTo("Transaction already exists with externalTxId: ext-tx-456")
    }

    @Test
    fun `should create VaultNotActiveException with correct code and message`() {
        // when
        val exception = VaultNotActiveException("vault-789")

        // then
        assertThat(exception.errorCode).isEqualTo(ErrorCode.VAULT_NOT_ACTIVE)
        assertThat(exception.message).isEqualTo("Vault vault-789 is not active")
    }

    @Test
    fun `should create InsufficientBalanceException with correct code and message`() {
        // when
        val exception = InsufficientBalanceException("vault-1", "ETH")

        // then
        assertThat(exception.errorCode).isEqualTo(ErrorCode.INSUFFICIENT_BALANCE)
        assertThat(exception.message).isEqualTo("Insufficient balance for asset ETH in vault vault-1")
    }

    @Test
    fun `should create InvalidTransactionStateException with details`() {
        // when
        val exception = InvalidTransactionStateException("tx-001", "PENDING", "COMPLETED")

        // then
        assertThat(exception.errorCode).isEqualTo(ErrorCode.INVALID_TRANSACTION_STATE)
        assertThat(exception.message).isEqualTo("Transaction tx-001 cannot transition from PENDING to COMPLETED")
    }

    @Test
    fun `should create FireblocksApiException with cause`() {
        // given
        val cause = RuntimeException("connection refused")

        // when
        val exception = FireblocksApiException("Fireblocks API call failed", cause)

        // then
        assertThat(exception.errorCode).isEqualTo(ErrorCode.FIREBLOCKS_API_ERROR)
        assertThat(exception.message).isEqualTo("Fireblocks API call failed")
        assertThat(exception.cause).isEqualTo(cause)
    }

    @Test
    fun `should preserve cause chain`() {
        // given
        val rootCause = SocketTimeoutException("read timed out")

        // when
        val exception = FireblocksTimeoutException("Fireblocks request timed out", rootCause)

        // then
        assertThat(exception.errorCode).isEqualTo(ErrorCode.FIREBLOCKS_TIMEOUT)
        assertThat(exception.cause).isEqualTo(rootCause)
        assertThat(exception).isInstanceOf(CustodyException::class.java)
        assertThat(exception).isInstanceOf(RuntimeException::class.java)
    }
}
