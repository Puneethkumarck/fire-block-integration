package com.stablecoin.custody.fireblocks.domain.wallet

import com.stablecoin.custody.fireblocks.domain.audit.AuditLogRepository
import com.stablecoin.custody.fireblocks.domain.audit.AuditOperation
import com.stablecoin.custody.fireblocks.domain.audit.AuditStatus
import com.stablecoin.custody.fireblocks.domain.event.AddressCreatedEvent
import com.stablecoin.custody.fireblocks.domain.event.WalletAssetCreatedEvent
import com.stablecoin.custody.fireblocks.domain.exception.AssetNotFoundException
import com.stablecoin.custody.fireblocks.domain.exception.VaultNotActiveException
import com.stablecoin.custody.fireblocks.domain.exception.VaultNotFoundException
import com.stablecoin.custody.fireblocks.domain.port.EventPublisher
import com.stablecoin.custody.fireblocks.domain.port.FireblocksVaultPort
import com.stablecoin.custody.fireblocks.domain.vault.VaultRepository
import com.stablecoin.custody.fireblocks.domain.vault.VaultStatus
import com.stablecoin.custody.fireblocks.test.fixtures.aDepositAddress
import com.stablecoin.custody.fireblocks.test.fixtures.aDepositAddressResult
import com.stablecoin.custody.fireblocks.test.fixtures.aGenerateAddressCommand
import com.stablecoin.custody.fireblocks.test.fixtures.aSupportedAsset
import com.stablecoin.custody.fireblocks.test.fixtures.aVault
import com.stablecoin.custody.fireblocks.test.fixtures.aWalletAsset
import com.stablecoin.custody.fireblocks.test.fixtures.aWalletAssetResult
import com.stablecoin.custody.fireblocks.test.fixtures.anActivateAssetCommand
import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class WalletAssetServiceTest {
    private val walletAssetRepository: WalletAssetRepository = mockk()
    private val depositAddressRepository: DepositAddressRepository = mockk()
    private val vaultRepository: VaultRepository = mockk()
    private val supportedAssetRepository: SupportedAssetRepository = mockk()
    private val fireblocksClient: FireblocksVaultPort = mockk()
    private val assetEventPublisher: EventPublisher<WalletAssetCreatedEvent> = mockk()
    private val addressEventPublisher: EventPublisher<AddressCreatedEvent> = mockk()
    private val auditLogRepository: AuditLogRepository = mockk()

    private val service =
        WalletAssetService(
            walletAssetRepository,
            depositAddressRepository,
            vaultRepository,
            supportedAssetRepository,
            fireblocksClient,
            assetEventPublisher,
            addressEventPublisher,
            auditLogRepository,
        )

    @Test
    fun `should activate asset in active vault`() {
        // given
        val vault = aVault(status = VaultStatus.ACTIVE, fireblocksVaultId = "fb-vault-123")
        val command = anActivateAssetCommand(vaultId = vault.id, currency = "BTC", protocol = "BTC")
        val supportedAsset = aSupportedAsset(currency = "BTC", protocol = "BTC", fireblocksAssetId = "BTC")
        val walletAssetResult = aWalletAssetResult()
        every { vaultRepository.findById(command.vaultId) } returns vault
        every { walletAssetRepository.findByVaultIdAndCurrencyAndProtocol(command.vaultId, "BTC", "BTC") } returns null
        every { supportedAssetRepository.findByCurrencyAndProtocol("BTC", "BTC") } returns supportedAsset
        every { fireblocksClient.createWalletAsset("fb-vault-123", "BTC") } returns walletAssetResult
        every { walletAssetRepository.save(any()) } returnsArgument 0
        every { assetEventPublisher.publish(any()) } just runs
        every { auditLogRepository.save(any()) } returnsArgument 0

        // when
        val result = service.activateAsset(command)

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
    fun `should return existing asset when already activated`() {
        // given
        val vault = aVault(status = VaultStatus.ACTIVE)
        val command = anActivateAssetCommand(vaultId = vault.id, currency = "BTC", protocol = "BTC")
        val existingAsset = aWalletAsset(vaultId = vault.id, currency = "BTC", protocol = "BTC")
        every { vaultRepository.findById(command.vaultId) } returns vault
        every { walletAssetRepository.findByVaultIdAndCurrencyAndProtocol(command.vaultId, "BTC", "BTC") } returns existingAsset

        // when
        val result = service.activateAsset(command)

        // then
        assertThat(result).usingRecursiveComparison().isEqualTo(existingAsset)
        verify(exactly = 0) { fireblocksClient.createWalletAsset(any(), any()) }
        verify(exactly = 0) { walletAssetRepository.save(any()) }
        verify(exactly = 0) { assetEventPublisher.publish(any()) }
        verify(exactly = 0) { auditLogRepository.save(any()) }
    }

    @Test
    fun `should throw VaultNotFoundException when vault does not exist`() {
        // given
        val command = anActivateAssetCommand()
        every { vaultRepository.findById(command.vaultId) } returns null

        // when / then
        assertThatThrownBy { service.activateAsset(command) }
            .isInstanceOf(VaultNotFoundException::class.java)
    }

    @Test
    fun `should throw when vault is not active`() {
        // given
        val vault = aVault(status = VaultStatus.PENDING, fireblocksVaultId = null)
        val command = anActivateAssetCommand(vaultId = vault.id)
        every { vaultRepository.findById(command.vaultId) } returns vault

        // when / then
        assertThatThrownBy { service.activateAsset(command) }
            .isInstanceOf(VaultNotActiveException::class.java)
    }

    @Test
    fun `should resolve currency and protocol to fireblocksAssetId via SupportedAssetRepository`() {
        // given
        val vault = aVault(status = VaultStatus.ACTIVE, fireblocksVaultId = "fb-vault-123")
        val command = anActivateAssetCommand(vaultId = vault.id, currency = "EURC", protocol = "ETH")
        val supportedAsset = aSupportedAsset(currency = "EURC", protocol = "ETH", fireblocksAssetId = "EURC")
        every { vaultRepository.findById(command.vaultId) } returns vault
        every { walletAssetRepository.findByVaultIdAndCurrencyAndProtocol(command.vaultId, "EURC", "ETH") } returns null
        every { supportedAssetRepository.findByCurrencyAndProtocol("EURC", "ETH") } returns supportedAsset
        every { fireblocksClient.createWalletAsset("fb-vault-123", "EURC") } returns aWalletAssetResult(id = "EURC")
        every { walletAssetRepository.save(any()) } returnsArgument 0
        every { assetEventPublisher.publish(any()) } just runs
        every { auditLogRepository.save(any()) } returnsArgument 0

        // when
        val result = service.activateAsset(command)

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
    fun `should call Fireblocks with resolved fireblocksAssetId`() {
        // given
        val vault = aVault(status = VaultStatus.ACTIVE, fireblocksVaultId = "fb-vault-999")
        val command = anActivateAssetCommand(vaultId = vault.id, currency = "ETH", protocol = "ETH")
        val supportedAsset = aSupportedAsset(currency = "ETH", protocol = "ETH", fireblocksAssetId = "ETH")
        every { vaultRepository.findById(command.vaultId) } returns vault
        every { walletAssetRepository.findByVaultIdAndCurrencyAndProtocol(command.vaultId, "ETH", "ETH") } returns null
        every { supportedAssetRepository.findByCurrencyAndProtocol("ETH", "ETH") } returns supportedAsset
        every { fireblocksClient.createWalletAsset("fb-vault-999", "ETH") } returns aWalletAssetResult(id = "ETH")
        every { walletAssetRepository.save(any()) } returnsArgument 0
        every { assetEventPublisher.publish(any()) } just runs
        every { auditLogRepository.save(any()) } returnsArgument 0

        // when
        service.activateAsset(command)

        // then
        verify { fireblocksClient.createWalletAsset("fb-vault-999", "ETH") }
    }

    @Test
    fun `should throw AssetNotFoundException for unsupported currency-protocol pair`() {
        // given
        val vault = aVault(status = VaultStatus.ACTIVE)
        val command = anActivateAssetCommand(vaultId = vault.id, currency = "XRP", protocol = "XRP")
        every { vaultRepository.findById(command.vaultId) } returns vault
        every { walletAssetRepository.findByVaultIdAndCurrencyAndProtocol(command.vaultId, "XRP", "XRP") } returns null
        every { supportedAssetRepository.findByCurrencyAndProtocol("XRP", "XRP") } returns null

        // when / then
        assertThatThrownBy { service.activateAsset(command) }
            .isInstanceOf(AssetNotFoundException::class.java)
    }

    @Test
    fun `should publish WalletAssetCreatedEvent`() {
        // given
        val vault = aVault(status = VaultStatus.ACTIVE, fireblocksVaultId = "fb-vault-123")
        val command = anActivateAssetCommand(vaultId = vault.id, currency = "BTC", protocol = "BTC")
        val supportedAsset = aSupportedAsset(currency = "BTC", protocol = "BTC", fireblocksAssetId = "BTC")
        every { vaultRepository.findById(command.vaultId) } returns vault
        every { walletAssetRepository.findByVaultIdAndCurrencyAndProtocol(command.vaultId, "BTC", "BTC") } returns null
        every { supportedAssetRepository.findByCurrencyAndProtocol("BTC", "BTC") } returns supportedAsset
        every { fireblocksClient.createWalletAsset("fb-vault-123", "BTC") } returns aWalletAssetResult()
        every { walletAssetRepository.save(any()) } returnsArgument 0
        every { assetEventPublisher.publish(any()) } just runs
        every { auditLogRepository.save(any()) } returnsArgument 0

        // when
        service.activateAsset(command)

        // then
        verify {
            assetEventPublisher.publish(
                match<WalletAssetCreatedEvent> {
                    it.vaultId == vault.id.value &&
                        it.currency == "BTC" &&
                        it.protocol == "BTC" &&
                        it.fireblocksAssetId == "BTC"
                },
            )
        }
    }

    @Test
    fun `should save audit log on asset activation`() {
        // given
        val vault = aVault(status = VaultStatus.ACTIVE, fireblocksVaultId = "fb-vault-123")
        val command = anActivateAssetCommand(vaultId = vault.id, currency = "BTC", protocol = "BTC")
        val supportedAsset = aSupportedAsset(currency = "BTC", protocol = "BTC", fireblocksAssetId = "BTC")
        every { vaultRepository.findById(command.vaultId) } returns vault
        every { walletAssetRepository.findByVaultIdAndCurrencyAndProtocol(command.vaultId, "BTC", "BTC") } returns null
        every { supportedAssetRepository.findByCurrencyAndProtocol("BTC", "BTC") } returns supportedAsset
        every { fireblocksClient.createWalletAsset("fb-vault-123", "BTC") } returns aWalletAssetResult()
        every { walletAssetRepository.save(any()) } returnsArgument 0
        every { assetEventPublisher.publish(any()) } just runs
        every { auditLogRepository.save(any()) } returnsArgument 0

        // when
        service.activateAsset(command)

        // then
        verify {
            auditLogRepository.save(
                match {
                    it.operation == AuditOperation.ASSET_ACTIVATED &&
                        it.status == AuditStatus.SUCCESS &&
                        it.actor == "system"
                },
            )
        }
    }

    @Test
    fun `should generate deposit address for active asset`() {
        // given
        val vault = aVault(status = VaultStatus.ACTIVE, fireblocksVaultId = "fb-vault-123")
        val walletAsset = aWalletAsset(vaultId = vault.id, currency = "BTC", protocol = "BTC", fireblocksAssetId = "BTC")
        val command = aGenerateAddressCommand(vaultId = vault.id, currency = "BTC", protocol = "BTC")
        val fbAddress = aDepositAddressResult(address = "bc1qxy2kgdygjrsqtzq2n0yrf2493p83kkfjhx0wlh")
        every { walletAssetRepository.findByVaultIdAndCurrencyAndProtocol(command.vaultId, "BTC", "BTC") } returns walletAsset
        every { depositAddressRepository.findByWalletAssetId(walletAsset.id) } returns null
        every { vaultRepository.findById(command.vaultId) } returns vault
        every { fireblocksClient.generateDepositAddress("fb-vault-123", "BTC") } returns fbAddress
        every { depositAddressRepository.save(any()) } returnsArgument 0
        every { addressEventPublisher.publish(any()) } just runs
        every { auditLogRepository.save(any()) } returnsArgument 0

        // when
        val result = service.generateDepositAddress(command)

        // then
        val expected =
            aDepositAddress(
                walletAssetId = walletAsset.id,
                address = "bc1qxy2kgdygjrsqtzq2n0yrf2493p83kkfjhx0wlh",
                tag = null,
                legacyAddress = null,
            )
        assertThat(result)
            .usingRecursiveComparison()
            .ignoringFields("id", "createdAt", "updatedAt")
            .isEqualTo(expected)
    }

    @Test
    fun `should use walletAsset fireblocksAssetId for address generation`() {
        // given
        val vault = aVault(status = VaultStatus.ACTIVE, fireblocksVaultId = "fb-vault-123")
        val walletAsset = aWalletAsset(vaultId = vault.id, currency = "EURC", protocol = "ETH", fireblocksAssetId = "EURC")
        val command = aGenerateAddressCommand(vaultId = vault.id, currency = "EURC", protocol = "ETH")
        val fbAddress = aDepositAddressResult()
        every { walletAssetRepository.findByVaultIdAndCurrencyAndProtocol(command.vaultId, "EURC", "ETH") } returns walletAsset
        every { depositAddressRepository.findByWalletAssetId(walletAsset.id) } returns null
        every { vaultRepository.findById(command.vaultId) } returns vault
        every { fireblocksClient.generateDepositAddress("fb-vault-123", "EURC") } returns fbAddress
        every { depositAddressRepository.save(any()) } returnsArgument 0
        every { addressEventPublisher.publish(any()) } just runs
        every { auditLogRepository.save(any()) } returnsArgument 0

        // when
        service.generateDepositAddress(command)

        // then
        verify { fireblocksClient.generateDepositAddress("fb-vault-123", "EURC") }
    }

    @Test
    fun `should return existing address when already generated`() {
        // given
        val vault = aVault(status = VaultStatus.ACTIVE)
        val walletAsset = aWalletAsset(vaultId = vault.id)
        val existingAddress = aDepositAddress(walletAssetId = walletAsset.id)
        val command = aGenerateAddressCommand(vaultId = vault.id, currency = "BTC", protocol = "BTC")
        every { walletAssetRepository.findByVaultIdAndCurrencyAndProtocol(command.vaultId, "BTC", "BTC") } returns walletAsset
        every { depositAddressRepository.findByWalletAssetId(walletAsset.id) } returns existingAddress

        // when
        val result = service.generateDepositAddress(command)

        // then
        assertThat(result).usingRecursiveComparison().isEqualTo(existingAddress)
        verify(exactly = 0) { fireblocksClient.generateDepositAddress(any(), any()) }
        verify(exactly = 0) { depositAddressRepository.save(any()) }
        verify(exactly = 0) { addressEventPublisher.publish(any()) }
        verify(exactly = 0) { auditLogRepository.save(any()) }
    }

    @Test
    fun `should throw AssetNotFoundException when asset not activated`() {
        // given
        val command = aGenerateAddressCommand()
        every { walletAssetRepository.findByVaultIdAndCurrencyAndProtocol(command.vaultId, "BTC", "BTC") } returns null

        // when / then
        assertThatThrownBy { service.generateDepositAddress(command) }
            .isInstanceOf(AssetNotFoundException::class.java)
    }

    @Test
    fun `should publish AddressCreatedEvent`() {
        // given
        val vault = aVault(status = VaultStatus.ACTIVE, fireblocksVaultId = "fb-vault-123")
        val walletAsset = aWalletAsset(vaultId = vault.id, currency = "BTC", protocol = "BTC", fireblocksAssetId = "BTC")
        val command = aGenerateAddressCommand(vaultId = vault.id, currency = "BTC", protocol = "BTC")
        val fbAddress = aDepositAddressResult()
        every { walletAssetRepository.findByVaultIdAndCurrencyAndProtocol(command.vaultId, "BTC", "BTC") } returns walletAsset
        every { depositAddressRepository.findByWalletAssetId(walletAsset.id) } returns null
        every { vaultRepository.findById(command.vaultId) } returns vault
        every { fireblocksClient.generateDepositAddress("fb-vault-123", "BTC") } returns fbAddress
        every { depositAddressRepository.save(any()) } returnsArgument 0
        every { addressEventPublisher.publish(any()) } just runs
        every { auditLogRepository.save(any()) } returnsArgument 0

        // when
        service.generateDepositAddress(command)

        // then
        verify {
            addressEventPublisher.publish(
                match<AddressCreatedEvent> {
                    it.vaultId == vault.id.value &&
                        it.address == "0x1234567890abcdef1234567890abcdef12345678"
                },
            )
        }
    }

    @Test
    fun `should save audit log on address generation`() {
        // given
        val vault = aVault(status = VaultStatus.ACTIVE, fireblocksVaultId = "fb-vault-123")
        val walletAsset = aWalletAsset(vaultId = vault.id, currency = "BTC", protocol = "BTC", fireblocksAssetId = "BTC")
        val command = aGenerateAddressCommand(vaultId = vault.id, currency = "BTC", protocol = "BTC")
        val fbAddress = aDepositAddressResult()
        every { walletAssetRepository.findByVaultIdAndCurrencyAndProtocol(command.vaultId, "BTC", "BTC") } returns walletAsset
        every { depositAddressRepository.findByWalletAssetId(walletAsset.id) } returns null
        every { vaultRepository.findById(command.vaultId) } returns vault
        every { fireblocksClient.generateDepositAddress("fb-vault-123", "BTC") } returns fbAddress
        every { depositAddressRepository.save(any()) } returnsArgument 0
        every { addressEventPublisher.publish(any()) } just runs
        every { auditLogRepository.save(any()) } returnsArgument 0

        // when
        service.generateDepositAddress(command)

        // then
        verify {
            auditLogRepository.save(
                match {
                    it.operation == AuditOperation.ADDRESS_GENERATED &&
                        it.status == AuditStatus.SUCCESS &&
                        it.actor == "system"
                },
            )
        }
    }

    @Test
    fun `should handle nullable tag in deposit address`() {
        // given
        val vault = aVault(status = VaultStatus.ACTIVE, fireblocksVaultId = "fb-vault-123")
        val walletAsset = aWalletAsset(vaultId = vault.id, currency = "XLM", protocol = "XLM", fireblocksAssetId = "XLM")
        val command = aGenerateAddressCommand(vaultId = vault.id, currency = "XLM", protocol = "XLM")
        val fbAddress =
            aDepositAddressResult(
                address = "GCZFMH32MF5EAWETZTKF3ZV5SEVJPI53UEMDNSW55WBR75GMZJU4U573",
                tag = "12345",
                legacyAddress = null,
            )
        every { walletAssetRepository.findByVaultIdAndCurrencyAndProtocol(command.vaultId, "XLM", "XLM") } returns walletAsset
        every { depositAddressRepository.findByWalletAssetId(walletAsset.id) } returns null
        every { vaultRepository.findById(command.vaultId) } returns vault
        every { fireblocksClient.generateDepositAddress("fb-vault-123", "XLM") } returns fbAddress
        every { depositAddressRepository.save(any()) } returnsArgument 0
        every { addressEventPublisher.publish(any()) } just runs
        every { auditLogRepository.save(any()) } returnsArgument 0

        // when
        val result = service.generateDepositAddress(command)

        // then
        val expected =
            aDepositAddress(
                walletAssetId = walletAsset.id,
                address = "GCZFMH32MF5EAWETZTKF3ZV5SEVJPI53UEMDNSW55WBR75GMZJU4U573",
                tag = "12345",
                legacyAddress = null,
            )
        assertThat(result)
            .usingRecursiveComparison()
            .ignoringFields("id", "createdAt", "updatedAt")
            .isEqualTo(expected)
    }
}
