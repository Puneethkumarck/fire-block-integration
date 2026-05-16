package com.stablecoin.custody.fireblocks.domain.wallet

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import com.stablecoin.custody.fireblocks.AbstractIntegrationTest
import com.stablecoin.custody.fireblocks.domain.audit.AuditLogRepository
import com.stablecoin.custody.fireblocks.domain.audit.AuditOperation
import com.stablecoin.custody.fireblocks.domain.audit.AuditStatus
import com.stablecoin.custody.fireblocks.domain.exception.AssetNotFoundException
import com.stablecoin.custody.fireblocks.domain.exception.VaultNotActiveException
import com.stablecoin.custody.fireblocks.domain.exception.VaultNotFoundException
import com.stablecoin.custody.fireblocks.domain.vault.VaultId
import com.stablecoin.custody.fireblocks.domain.vault.VaultRepository
import com.stablecoin.custody.fireblocks.domain.vault.VaultStatus
import com.stablecoin.custody.fireblocks.test.fixtures.aDepositAddress
import com.stablecoin.custody.fireblocks.test.fixtures.aGenerateAddressCommand
import com.stablecoin.custody.fireblocks.test.fixtures.aVault
import com.stablecoin.custody.fireblocks.test.fixtures.aWalletAsset
import com.stablecoin.custody.fireblocks.test.fixtures.anActivateAssetCommand
import com.stablecoin.custody.fireblocks.test.fixtures.anAuditLog
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import java.util.UUID

class WalletAssetServiceIntegrationTest : AbstractIntegrationTest() {
    @Autowired
    private lateinit var walletAssetService: WalletAssetService

    @Autowired
    private lateinit var vaultRepository: VaultRepository

    @Autowired
    private lateinit var walletAssetRepository: WalletAssetRepository

    @Autowired
    private lateinit var depositAddressRepository: DepositAddressRepository

    @Autowired
    private lateinit var auditLogRepository: AuditLogRepository

    companion object {
        private val wireMock = WireMockServer(wireMockConfig().dynamicPort())

        @JvmStatic
        @BeforeAll
        fun startWireMock() {
            wireMock.start()
        }

        @JvmStatic
        @AfterAll
        fun stopWireMock() {
            wireMock.stop()
        }

        @JvmStatic
        @DynamicPropertySource
        fun configureWireMock(registry: DynamicPropertyRegistry) {
            registry.add("fireblocks.api.base-url") { "http://localhost:${wireMock.port()}" }
        }
    }

    @BeforeEach
    fun resetWireMock() {
        wireMock.resetAll()
    }

    @Test
    fun `should activate asset end-to-end`() {
        // given
        val vault = vaultRepository.save(aVault(fireblocksVaultId = "fb-int-001"))
        val command = anActivateAssetCommand(vaultId = vault.id, currency = "BTC", protocol = "BTC")
        wireMock.stubFor(
            post(urlPathEqualTo("/v1/vault/accounts/fb-int-001/BTC"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("""{"id":"BTC","available":"0"}"""),
                ),
        )

        // when
        val result = walletAssetService.activateAsset(command)

        // then
        val expected =
            aWalletAsset(
                vaultId = vault.id,
                currency = "BTC",
                protocol = "BTC",
                fireblocksAssetId = "BTC",
                status = AssetStatus.ACTIVE,
            )
        assertThat(result)
            .usingRecursiveComparison()
            .ignoringFields("id", "createdAt", "updatedAt")
            .isEqualTo(expected)
    }

    @Test
    fun `should resolve currency and protocol to fireblocksAssetId end-to-end`() {
        // given
        val vault = vaultRepository.save(aVault(fireblocksVaultId = "fb-int-002"))
        val command = anActivateAssetCommand(vaultId = vault.id, currency = "EURC", protocol = "ETH")
        wireMock.stubFor(
            post(urlPathEqualTo("/v1/vault/accounts/fb-int-002/EURC"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("""{"id":"EURC","available":"0"}"""),
                ),
        )

        // when
        val result = walletAssetService.activateAsset(command)

        // then
        val expected =
            aWalletAsset(
                vaultId = vault.id,
                currency = "EURC",
                protocol = "ETH",
                fireblocksAssetId = "EURC",
                status = AssetStatus.ACTIVE,
            )
        assertThat(result)
            .usingRecursiveComparison()
            .ignoringFields("id", "createdAt", "updatedAt")
            .isEqualTo(expected)
    }

    @Test
    fun `should generate deposit address end-to-end`() {
        // given
        val vault = vaultRepository.save(aVault(fireblocksVaultId = "fb-int-003"))
        val activateCommand = anActivateAssetCommand(vaultId = vault.id, currency = "ETH", protocol = "ETH")
        wireMock.stubFor(
            post(urlPathEqualTo("/v1/vault/accounts/fb-int-003/ETH"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("""{"id":"ETH","available":"0"}"""),
                ),
        )
        walletAssetService.activateAsset(activateCommand)

        val addressCommand = aGenerateAddressCommand(vaultId = vault.id, currency = "ETH", protocol = "ETH")
        wireMock.stubFor(
            post(urlPathEqualTo("/v1/vault/accounts/fb-int-003/ETH/addresses"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("""{"address":"0xabc123def456","tag":null,"legacyAddress":null}"""),
                ),
        )

        // when
        val result = walletAssetService.generateDepositAddress(addressCommand)

        // then
        val expected =
            aDepositAddress(
                walletAssetId = result.walletAssetId,
                address = "0xabc123def456",
                tag = null,
                legacyAddress = null,
            )
        assertThat(result)
            .usingRecursiveComparison()
            .ignoringFields("id", "createdAt", "updatedAt")
            .isEqualTo(expected)
    }

    @Test
    fun `should return existing asset for duplicate activation`() {
        // given
        val vault = vaultRepository.save(aVault(fireblocksVaultId = "fb-int-004"))
        val command = anActivateAssetCommand(vaultId = vault.id, currency = "SOL", protocol = "SOL")
        wireMock.stubFor(
            post(urlPathEqualTo("/v1/vault/accounts/fb-int-004/SOL"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("""{"id":"SOL","available":"0"}"""),
                ),
        )
        val first = walletAssetService.activateAsset(command)

        // when
        val second = walletAssetService.activateAsset(command)

        // then
        assertThat(second.id).isEqualTo(first.id)
    }

    @Test
    fun `should return existing address for duplicate generation`() {
        // given
        val vault = vaultRepository.save(aVault(fireblocksVaultId = "fb-int-005"))
        val activateCommand = anActivateAssetCommand(vaultId = vault.id, currency = "BTC", protocol = "BTC")
        wireMock.stubFor(
            post(urlPathEqualTo("/v1/vault/accounts/fb-int-005/BTC"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("""{"id":"BTC","available":"0"}"""),
                ),
        )
        walletAssetService.activateAsset(activateCommand)

        val addressCommand = aGenerateAddressCommand(vaultId = vault.id, currency = "BTC", protocol = "BTC")
        wireMock.stubFor(
            post(urlPathEqualTo("/v1/vault/accounts/fb-int-005/BTC/addresses"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("""{"address":"bc1qfirstaddress","tag":null,"legacyAddress":null}"""),
                ),
        )
        val first = walletAssetService.generateDepositAddress(addressCommand)

        // when
        val second = walletAssetService.generateDepositAddress(addressCommand)

        // then
        assertThat(second.id).isEqualTo(first.id)
    }

    @Test
    fun `should reject asset activation for non-existent vault`() {
        // given
        val command = anActivateAssetCommand(vaultId = VaultId(UUID.randomUUID()), currency = "BTC", protocol = "BTC")

        // when / then
        assertThatThrownBy { walletAssetService.activateAsset(command) }
            .isInstanceOf(VaultNotFoundException::class.java)
    }

    @Test
    fun `should reject asset activation for unsupported currency-protocol pair`() {
        // given
        val vault = vaultRepository.save(aVault(fireblocksVaultId = "fb-int-006"))
        val command = anActivateAssetCommand(vaultId = vault.id, currency = "XRP", protocol = "XRP")

        // when / then
        assertThatThrownBy { walletAssetService.activateAsset(command) }
            .isInstanceOf(AssetNotFoundException::class.java)
    }

    @Test
    fun `should reject address generation for non-activated asset`() {
        // given
        val vault = vaultRepository.save(aVault(fireblocksVaultId = "fb-int-007"))
        val command = aGenerateAddressCommand(vaultId = vault.id, currency = "BTC", protocol = "BTC")

        // when / then
        assertThatThrownBy { walletAssetService.generateDepositAddress(command) }
            .isInstanceOf(AssetNotFoundException::class.java)
    }

    @Test
    fun `should reject asset activation for inactive vault`() {
        // given
        val vault = vaultRepository.save(aVault(status = VaultStatus.PENDING, fireblocksVaultId = null))
        val command = anActivateAssetCommand(vaultId = vault.id, currency = "BTC", protocol = "BTC")

        // when / then
        assertThatThrownBy { walletAssetService.activateAsset(command) }
            .isInstanceOf(VaultNotActiveException::class.java)
    }

    @Test
    fun `should persist audit log on asset activation`() {
        // given
        val vault = vaultRepository.save(aVault(fireblocksVaultId = "fb-int-009"))
        val command = anActivateAssetCommand(vaultId = vault.id, currency = "BTC", protocol = "BTC")
        wireMock.stubFor(
            post(urlPathEqualTo("/v1/vault/accounts/fb-int-009/BTC"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("""{"id":"BTC","available":"0"}"""),
                ),
        )

        // when
        val result = walletAssetService.activateAsset(command)

        // then
        val auditLogs = auditLogRepository.findByResourceId(result.id.value.toString())
        val expected =
            anAuditLog(
                operation = AuditOperation.ASSET_ACTIVATED,
                status = AuditStatus.SUCCESS,
                actor = "system",
                resourceId = result.id.value.toString(),
            )
        assertThat(auditLogs.first())
            .usingRecursiveComparison()
            .ignoringFields("id", "timestamp", "fireblocksRequestId", "details")
            .isEqualTo(expected)
    }

    @Test
    fun `should persist audit log on address generation`() {
        // given
        val vault = vaultRepository.save(aVault(fireblocksVaultId = "fb-int-010"))
        val activateCommand = anActivateAssetCommand(vaultId = vault.id, currency = "ETH", protocol = "ETH")
        wireMock.stubFor(
            post(urlPathEqualTo("/v1/vault/accounts/fb-int-010/ETH"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("""{"id":"ETH","available":"0"}"""),
                ),
        )
        walletAssetService.activateAsset(activateCommand)

        val addressCommand = aGenerateAddressCommand(vaultId = vault.id, currency = "ETH", protocol = "ETH")
        wireMock.stubFor(
            post(urlPathEqualTo("/v1/vault/accounts/fb-int-010/ETH/addresses"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .withBody("""{"address":"0xaudit123","tag":null,"legacyAddress":null}"""),
                ),
        )

        // when
        val result = walletAssetService.generateDepositAddress(addressCommand)

        // then
        val auditLogs = auditLogRepository.findByResourceId(result.id.value.toString())
        val expected =
            anAuditLog(
                operation = AuditOperation.ADDRESS_GENERATED,
                status = AuditStatus.SUCCESS,
                actor = "system",
                resourceId = result.id.value.toString(),
            )
        assertThat(auditLogs.first())
            .usingRecursiveComparison()
            .ignoringFields("id", "timestamp", "fireblocksRequestId", "details")
            .isEqualTo(expected)
    }
}
