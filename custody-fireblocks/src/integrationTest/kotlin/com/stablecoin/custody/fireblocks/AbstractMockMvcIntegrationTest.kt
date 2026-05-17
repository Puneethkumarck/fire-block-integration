package com.stablecoin.custody.fireblocks

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.servlet.MockMvc

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
abstract class AbstractMockMvcIntegrationTest : AbstractIntegrationTest() {
    @Autowired
    protected lateinit var mockMvc: MockMvc

    companion object {
        @JvmStatic
        val wireMock: WireMockServer = WireMockServer(wireMockConfig().dynamicPort()).apply { start() }

        @JvmStatic
        @DynamicPropertySource
        fun configureMockMvcProperties(registry: DynamicPropertyRegistry) {
            registry.add("fireblocks.api.base-url") { "http://localhost:${wireMock.port()}" }
        }
    }
}
