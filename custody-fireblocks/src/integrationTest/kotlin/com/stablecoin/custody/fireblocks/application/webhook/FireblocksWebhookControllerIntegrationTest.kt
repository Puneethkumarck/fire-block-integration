package com.stablecoin.custody.fireblocks.application.webhook

import com.stablecoin.custody.fireblocks.AbstractMockMvcIntegrationTest
import com.stablecoin.custody.fireblocks.domain.transaction.TransactionRepository
import com.stablecoin.custody.fireblocks.domain.transaction.TransactionStatus
import com.stablecoin.custody.fireblocks.test.fixtures.aTransaction
import com.stablecoin.custody.fireblocks.test.fixtures.aWebhookBody
import com.stablecoin.custody.fireblocks.test.fixtures.signWebhookBody
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.Instant
import java.util.Base64

class FireblocksWebhookControllerIntegrationTest : AbstractMockMvcIntegrationTest() {
    @Autowired
    private lateinit var transactionRepository: TransactionRepository

    @Test
    fun `should accept webhook with valid signature`() {
        // given
        val body = aWebhookBody(fireblocksTxId = "fb-unknown", status = "COMPLETED", txHash = """"0xhash"""")

        // when / then
        mockMvc
            .perform(
                post("/api/v1/webhooks/fireblocks")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Fireblocks-Signature", signWebhookBody(body, webhookKeyPair))
                    .content(body),
            ).andExpect(status().isOk)
    }

    @Test
    fun `should reject webhook with invalid signature`() {
        // given
        val body = aWebhookBody(fireblocksTxId = "fb-001", status = "COMPLETED")
        val invalidSignature =
            Base64.getEncoder().encodeToString("invalid".toByteArray())

        // when / then
        mockMvc
            .perform(
                post("/api/v1/webhooks/fireblocks")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Fireblocks-Signature", invalidSignature)
                    .content(body),
            ).andExpect(status().isUnauthorized)
    }

    @Test
    fun `should reject webhook with replayed timestamp`() {
        // given
        val oldTimestamp = Instant.now().minusSeconds(301).toEpochMilli()
        val body = aWebhookBody(fireblocksTxId = "fb-001", status = "COMPLETED", timestamp = oldTimestamp)

        // when / then
        mockMvc
            .perform(
                post("/api/v1/webhooks/fireblocks")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Fireblocks-Signature", signWebhookBody(body, webhookKeyPair))
                    .content(body),
            ).andExpect(status().isUnauthorized)
    }

    @Test
    fun `should process transaction status update end-to-end`() {
        // given
        transactionRepository.save(
            aTransaction(
                fireblocksTransactionId = "fb-e2e-001",
                status = TransactionStatus.SUBMITTED,
            ),
        )
        val body = aWebhookBody(fireblocksTxId = "fb-e2e-001", status = "BROADCASTING")

        // when
        mockMvc
            .perform(
                post("/api/v1/webhooks/fireblocks")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Fireblocks-Signature", signWebhookBody(body, webhookKeyPair))
                    .content(body),
            ).andExpect(status().isOk)

        // then
        val updated = transactionRepository.findByFireblocksTransactionId("fb-e2e-001")
        assertThat(updated).isNotNull
        assertThat(updated!!.status).isEqualTo(TransactionStatus.PROCESSING)
    }

    @Test
    fun `should return 200 for unknown transaction`() {
        // given
        val body = aWebhookBody(fireblocksTxId = "fb-nonexistent", status = "COMPLETED")

        // when / then
        mockMvc
            .perform(
                post("/api/v1/webhooks/fireblocks")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Fireblocks-Signature", signWebhookBody(body, webhookKeyPair))
                    .content(body),
            ).andExpect(status().isOk)
    }

    @Test
    fun `should return 200 for terminal re-delivery`() {
        // given
        transactionRepository.save(
            aTransaction(
                fireblocksTransactionId = "fb-terminal-001",
                status = TransactionStatus.CONFIRMED,
            ),
        )
        val body =
            aWebhookBody(
                fireblocksTxId = "fb-terminal-001",
                status = "COMPLETED",
                subStatus = """"CONFIRMED"""",
                txHash = """"0xterm"""",
            )

        // when
        mockMvc
            .perform(
                post("/api/v1/webhooks/fireblocks")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Fireblocks-Signature", signWebhookBody(body, webhookKeyPair))
                    .content(body),
            ).andExpect(status().isOk)

        // then
        val unchanged = transactionRepository.findByFireblocksTransactionId("fb-terminal-001")
        assertThat(unchanged).isNotNull
        assertThat(unchanged!!.status).isEqualTo(TransactionStatus.CONFIRMED)
    }
}
