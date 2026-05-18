package com.stablecoin.custody.fireblocks

import com.stablecoin.custody.fireblocks.test.containers.SharedTestContainers
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
abstract class AbstractIntegrationTest {
    companion object {
        @JvmStatic
        val webhookKeyPair = SharedTestContainers.webhookKeyPair

        @JvmStatic
        @DynamicPropertySource
        fun configureProperties(registry: DynamicPropertyRegistry) {
            SharedTestContainers.configureProperties(registry)
        }
    }
}
