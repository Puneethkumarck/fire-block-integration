package com.stablecoin.custody.fireblocks.application.controller

import com.stablecoin.custody.fireblocks.application.exception.GlobalExceptionHandler
import com.stablecoin.custody.fireblocks.domain.exception.VaultNotFoundException
import com.stablecoin.custody.fireblocks.domain.vault.CreateVaultCommand
import com.stablecoin.custody.fireblocks.domain.vault.VaultCreationService
import com.stablecoin.custody.fireblocks.domain.vault.VaultId
import com.stablecoin.custody.fireblocks.domain.vault.VaultQueryService
import com.stablecoin.custody.fireblocks.test.fixtures.aVault
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.util.UUID

class VaultControllerTest {
    private val vaultCreationService: VaultCreationService = mockk()
    private val vaultQueryService: VaultQueryService = mockk()
    private val controller = VaultController(vaultCreationService, vaultQueryService)
    private lateinit var mockMvc: MockMvc

    @BeforeEach
    fun setUp() {
        mockMvc =
            MockMvcBuilders
                .standaloneSetup(controller)
                .setMessageConverters(JacksonJsonHttpMessageConverter())
                .setControllerAdvice(GlobalExceptionHandler())
                .build()
    }

    @Test
    fun `should create vault and return 201`() {
        // given
        val vault = aVault()
        val command = CreateVaultCommand(customerRefId = "customer-ref-001", name = "Test Vault")
        every { vaultCreationService.createVault(command) } returns vault

        // when / then
        mockMvc
            .perform(
                post("/api/v1/vaults")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"customerRefId":"customer-ref-001","name":"Test Vault"}"""),
            ).andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").value(vault.id.value.toString()))
            .andExpect(jsonPath("$.fireblocksVaultId").value(vault.fireblocksVaultId))
            .andExpect(jsonPath("$.customerRefId").value(vault.customerRefId))
            .andExpect(jsonPath("$.name").value(vault.name))
            .andExpect(jsonPath("$.status").value(vault.status.name))
    }

    @Test
    fun `should return existing vault on duplicate customerRefId`() {
        // given
        val vault = aVault()
        val command = CreateVaultCommand(customerRefId = "customer-ref-001", name = "Test Vault")
        every { vaultCreationService.createVault(command) } returns vault

        // when / then
        mockMvc
            .perform(
                post("/api/v1/vaults")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"customerRefId":"customer-ref-001","name":"Test Vault"}"""),
            ).andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").value(vault.id.value.toString()))

        verify(exactly = 1) { vaultCreationService.createVault(command) }
    }

    @Test
    fun `should get vault by ID and return 200`() {
        // given
        val vaultId = UUID.randomUUID()
        val vault = aVault(id = VaultId(vaultId))
        every { vaultQueryService.getVault(VaultId(vaultId)) } returns vault

        // when / then
        mockMvc
            .perform(get("/api/v1/vaults/$vaultId"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(vaultId.toString()))
            .andExpect(jsonPath("$.customerRefId").value(vault.customerRefId))
            .andExpect(jsonPath("$.name").value(vault.name))
            .andExpect(jsonPath("$.status").value(vault.status.name))
    }

    @Test
    fun `should return 404 when vault not found`() {
        // given
        val vaultId = UUID.randomUUID()
        every { vaultQueryService.getVault(VaultId(vaultId)) } throws VaultNotFoundException(vaultId.toString())

        // when / then
        mockMvc
            .perform(get("/api/v1/vaults/$vaultId"))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `should return 400 when request body invalid`() {
        // when / then
        mockMvc
            .perform(
                post("/api/v1/vaults")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"customerRefId":"","name":""}"""),
            ).andExpect(status().isBadRequest)
    }

    @Test
    fun `should map request to command correctly`() {
        // given
        val vault = aVault()
        val command = CreateVaultCommand(customerRefId = "ref-123", name = "My Vault")
        every { vaultCreationService.createVault(command) } returns vault

        // when / then
        mockMvc
            .perform(
                post("/api/v1/vaults")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"customerRefId":"ref-123","name":"My Vault"}"""),
            ).andExpect(status().isCreated)

        verify { vaultCreationService.createVault(command) }
    }

    @Test
    fun `should map domain vault to response correctly`() {
        // given
        val vault = aVault(fireblocksVaultId = "fb-123", customerRefId = "ref-abc", name = "Vault A")
        val command = CreateVaultCommand(customerRefId = "ref-abc", name = "Vault A")
        every { vaultCreationService.createVault(command) } returns vault

        // when / then
        mockMvc
            .perform(
                post("/api/v1/vaults")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"customerRefId":"ref-abc","name":"Vault A"}"""),
            ).andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").value(vault.id.value.toString()))
            .andExpect(jsonPath("$.fireblocksVaultId").value("fb-123"))
            .andExpect(jsonPath("$.customerRefId").value("ref-abc"))
            .andExpect(jsonPath("$.name").value("Vault A"))
            .andExpect(jsonPath("$.status").value(vault.status.name))
            .andExpect(jsonPath("$.createdAt").exists())
            .andExpect(jsonPath("$.updatedAt").exists())
    }
}
