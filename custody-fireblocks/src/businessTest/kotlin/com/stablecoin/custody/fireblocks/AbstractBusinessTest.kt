package com.stablecoin.custody.fireblocks

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import com.stablecoin.custody.fireblocks.client.CustodyClientAutoConfiguration
import com.stablecoin.custody.fireblocks.test.containers.SharedTestContainers
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.servlet.MockMvc

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@EnableAutoConfiguration(exclude = [CustodyClientAutoConfiguration::class])
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
abstract class AbstractBusinessTest {
    @Autowired
    protected lateinit var mockMvc: MockMvc

    companion object {
        @JvmStatic
        val wireMock: WireMockServer = WireMockServer(wireMockConfig().dynamicPort()).apply { start() }

        @JvmStatic
        @DynamicPropertySource
        fun configureProperties(registry: DynamicPropertyRegistry) {
            SharedTestContainers.configureProperties(registry)
            registry.add("fireblocks.api.base-url") { "http://localhost:${wireMock.port()}" }
        }
    }
}
