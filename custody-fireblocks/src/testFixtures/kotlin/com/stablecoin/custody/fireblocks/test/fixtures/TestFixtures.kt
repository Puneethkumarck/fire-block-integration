package com.stablecoin.custody.fireblocks.test.fixtures

import java.util.UUID

fun randomId(): UUID = UUID.randomUUID()

fun randomString(prefix: String = "test"): String = "$prefix-${UUID.randomUUID().toString().take(8)}"
