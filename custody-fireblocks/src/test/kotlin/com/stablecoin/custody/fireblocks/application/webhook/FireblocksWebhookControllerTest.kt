package com.stablecoin.custody.fireblocks.application.webhook

import com.stablecoin.custody.fireblocks.domain.webhook.WebhookEventHandler
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders

class FireblocksWebhookControllerTest {
    private val webhookEventHandler: WebhookEventHandler = mockk(relaxed = true)
    private val controller = FireblocksWebhookController(webhookEventHandler)
    private lateinit var mockMvc: MockMvc

    @BeforeEach
    fun setUp() {
        mockMvc =
            MockMvcBuilders
                .standaloneSetup(controller)
                .setMessageConverters(JacksonJsonHttpMessageConverter())
                .build()
    }

    @Test
    fun `should delegate to WebhookEventHandler and return 200`() {
        // when / then
        mockMvc
            .perform(
                post("/api/v1/webhooks/fireblocks")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """{"type":"TRANSACTION_STATUS_UPDATED","tenantId":"t1","timestamp":1700000000000,"createdAt":1700000000000,"data":{"id":"fb-tx-001","status":"COMPLETED","subStatus":"CONFIRMED","txHash":"0xhash123"}}""",
                    ),
            ).andExpect(status().isOk)

        verify { webhookEventHandler.handle(any()) }
    }

    @Test
    fun `should return 200 regardless of payload type`() {
        // when / then
        mockMvc
            .perform(
                post("/api/v1/webhooks/fireblocks")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """{"type":"VAULT_CREATED","tenantId":"t1","timestamp":1700000000000,"createdAt":1700000000000,"data":null}""",
                    ),
            ).andExpect(status().isOk)

        verify { webhookEventHandler.handle(any()) }
    }

    @Test
    fun `should return 200 for unknown type with non-transaction data`() {
        // when / then
        mockMvc
            .perform(
                post("/api/v1/webhooks/fireblocks")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """{"type":"VAULT_CREATED","tenantId":"t1","timestamp":1700000000000,"createdAt":1700000000000,"data":{"foo":"bar"}}""",
                    ),
            ).andExpect(status().isOk)

        verify { webhookEventHandler.handle(any()) }
    }
}
