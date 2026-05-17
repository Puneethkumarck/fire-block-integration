package com.stablecoin.custody.fireblocks.application.webhook

import com.stablecoin.custody.fireblocks.domain.shared.logger
import com.stablecoin.custody.fireblocks.domain.webhook.WebhookEventHandler
import com.stablecoin.custody.fireblocks.domain.webhook.WebhookPayload
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

private val log = logger<FireblocksWebhookController>()

@RestController
@RequestMapping("/api/v1/webhooks")
class FireblocksWebhookController(
    private val webhookEventHandler: WebhookEventHandler,
) {
    @PostMapping("/fireblocks")
    fun handleWebhook(
        @RequestBody payload: WebhookPayload,
    ): ResponseEntity<Void> {
        log.info("Webhook received: type={}, fireblocksTxId={}", payload.type, payload.data?.id)
        webhookEventHandler.handle(payload)
        return ResponseEntity.ok().build()
    }
}
