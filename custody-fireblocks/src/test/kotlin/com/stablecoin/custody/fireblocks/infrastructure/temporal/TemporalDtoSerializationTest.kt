package com.stablecoin.custody.fireblocks.infrastructure.temporal

import com.fasterxml.jackson.databind.ObjectMapper
import com.stablecoin.custody.fireblocks.infrastructure.temporal.dto.CreateTransactionResult
import com.stablecoin.custody.fireblocks.infrastructure.temporal.dto.PollStatusResult
import com.stablecoin.custody.fireblocks.infrastructure.temporal.dto.ReserveFundsResult
import com.stablecoin.custody.fireblocks.infrastructure.temporal.dto.SubmitResult
import com.stablecoin.custody.fireblocks.infrastructure.temporal.dto.TemporalTransactionStatusSignal
import com.stablecoin.custody.fireblocks.test.fixtures.aFireblocksSubmitActivityCommand
import com.stablecoin.custody.fireblocks.test.fixtures.aStartTransactionRequest
import com.stablecoin.custody.fireblocks.test.fixtures.aTemporalTransactionStatusSignal
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.util.UUID

class TemporalDtoSerializationTest {
    private val objectMapper: ObjectMapper = ObjectMapper().findAndRegisterModules()

    @Test
    fun `should serialize and deserialize StartTransactionRequest`() {
        // given
        val request = aStartTransactionRequest()

        // when
        val json = objectMapper.writeValueAsString(request)
        val deserialized = objectMapper.readValue(json, request::class.java)

        // then
        assertThat(deserialized).isEqualTo(request)
    }

    @Test
    fun `should serialize and deserialize TemporalTransactionStatusSignal`() {
        // given
        val signal = aTemporalTransactionStatusSignal(txHash = "0xabc123")

        // when
        val json = objectMapper.writeValueAsString(signal)
        val deserialized = objectMapper.readValue(json, TemporalTransactionStatusSignal::class.java)

        // then
        assertThat(deserialized).isEqualTo(signal)
    }

    @Test
    fun `should serialize and deserialize TemporalTransactionStatusSignal with null fields`() {
        // given
        val signal = aTemporalTransactionStatusSignal(subStatus = null, txHash = null)

        // when
        val json = objectMapper.writeValueAsString(signal)
        val deserialized = objectMapper.readValue(json, TemporalTransactionStatusSignal::class.java)

        // then
        assertThat(deserialized).isEqualTo(signal)
    }

    @Test
    fun `should serialize and deserialize FireblocksSubmitActivityCommand`() {
        // given
        val command = aFireblocksSubmitActivityCommand()

        // when
        val json = objectMapper.writeValueAsString(command)
        val deserialized = objectMapper.readValue(json, command::class.java)

        // then
        assertThat(deserialized).isEqualTo(command)
    }

    @Test
    fun `should serialize and deserialize CreateTransactionResult`() {
        // given
        val result = CreateTransactionResult(transactionId = UUID.randomUUID().toString())

        // when
        val json = objectMapper.writeValueAsString(result)
        val deserialized = objectMapper.readValue(json, CreateTransactionResult::class.java)

        // then
        assertThat(deserialized).isEqualTo(result)
    }

    @Test
    fun `should serialize and deserialize SubmitResult`() {
        // given
        val result =
            SubmitResult(
                fireblocksTransactionId = "fb-tx-123",
                status = "SUBMITTED",
                subStatus = null,
                txHash = null,
            )

        // when
        val json = objectMapper.writeValueAsString(result)
        val deserialized = objectMapper.readValue(json, SubmitResult::class.java)

        // then
        assertThat(deserialized).isEqualTo(result)
    }

    @Test
    fun `should serialize and deserialize PollStatusResult`() {
        // given
        val result =
            PollStatusResult(
                fireblocksStatus = "CONFIRMING",
                subStatus = "PENDING_BLOCKCHAIN_CONFIRMATIONS",
                txHash = "0xabc123",
            )

        // when
        val json = objectMapper.writeValueAsString(result)
        val deserialized = objectMapper.readValue(json, PollStatusResult::class.java)

        // then
        assertThat(deserialized).isEqualTo(result)
    }

    @Test
    fun `should serialize and deserialize ReserveFundsResult`() {
        // given
        val result = ReserveFundsResult(allocationId = UUID.randomUUID().toString())

        // when
        val json = objectMapper.writeValueAsString(result)
        val deserialized = objectMapper.readValue(json, ReserveFundsResult::class.java)

        // then
        assertThat(deserialized).isEqualTo(result)
    }

    @Test
    fun `should preserve BigDecimal precision in StartTransactionRequest`() {
        // given
        val request = aStartTransactionRequest(amount = BigDecimal("123456789.123456789012345678"))

        // when
        val json = objectMapper.writeValueAsString(request)
        val deserialized = objectMapper.readValue(json, request::class.java)

        // then
        assertThat(deserialized.amount.compareTo(request.amount)).isZero()
    }
}
