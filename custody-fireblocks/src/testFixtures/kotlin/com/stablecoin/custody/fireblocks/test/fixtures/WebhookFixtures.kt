package com.stablecoin.custody.fireblocks.test.fixtures

import java.security.KeyPair
import java.security.Signature
import java.time.Instant
import java.util.Base64

fun aWebhookBody(
    fireblocksTxId: String = "fb-tx-001",
    status: String = "COMPLETED",
    subStatus: String = "null",
    txHash: String = "null",
    timestamp: Long = Instant.now().toEpochMilli(),
): String =
    """{"type":"TRANSACTION_STATUS_UPDATED","createdAt":$timestamp,""" +
        """"data":{"id":"$fireblocksTxId","status":"$status",""" +
        """"subStatus":$subStatus,"txHash":$txHash}}"""

fun signWebhookBody(
    body: String,
    keyPair: KeyPair,
): String {
    val sig = Signature.getInstance("SHA512withRSA")
    sig.initSign(keyPair.private)
    sig.update(body.toByteArray())
    return Base64.getEncoder().encodeToString(sig.sign())
}
