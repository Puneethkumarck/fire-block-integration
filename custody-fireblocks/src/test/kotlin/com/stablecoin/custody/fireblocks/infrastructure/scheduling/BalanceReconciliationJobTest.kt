package com.stablecoin.custody.fireblocks.infrastructure.scheduling

import com.stablecoin.custody.fireblocks.domain.audit.AuditLogRepository
import com.stablecoin.custody.fireblocks.domain.event.ReconciliationBreakDetectedEvent
import com.stablecoin.custody.fireblocks.domain.exception.FireblocksApiException
import com.stablecoin.custody.fireblocks.domain.port.BalanceResult
import com.stablecoin.custody.fireblocks.domain.port.EventPublisher
import com.stablecoin.custody.fireblocks.domain.port.FireblocksBalancePort
import com.stablecoin.custody.fireblocks.domain.reconciliation.BalanceReconciliationService
import com.stablecoin.custody.fireblocks.domain.reconciliation.InternalBalanceRepository
import com.stablecoin.custody.fireblocks.domain.reconciliation.ReconciliationResultRepository
import com.stablecoin.custody.fireblocks.domain.reconciliation.ReconciliationStatus
import com.stablecoin.custody.fireblocks.domain.vault.VaultRepository
import com.stablecoin.custody.fireblocks.domain.wallet.WalletAssetRepository
import com.stablecoin.custody.fireblocks.test.fixtures.aVault
import com.stablecoin.custody.fireblocks.test.fixtures.aWalletAsset
import com.stablecoin.custody.fireblocks.test.fixtures.anInternalBalance
import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.math.BigDecimal

@ExtendWith(MockKExtension::class)
class BalanceReconciliationJobTest {
    private val reconciliationService = BalanceReconciliationService()
    private val vaultRepository: VaultRepository = mockk()
    private val walletAssetRepository: WalletAssetRepository = mockk()
    private val internalBalanceRepository: InternalBalanceRepository = mockk()
    private val reconciliationResultRepository: ReconciliationResultRepository = mockk()
    private val fireblocksBalancePort: FireblocksBalancePort = mockk()
    private val auditLogRepository: AuditLogRepository = mockk()
    private val breakEventPublisher: EventPublisher<ReconciliationBreakDetectedEvent> = mockk()
    private val properties =
        ReconciliationProperties(
            enabled = true,
            interval = 900000,
            defaultTolerance = BigDecimal("0.01"),
            tolerances = mapOf("BTC" to BigDecimal("0.00001")),
        )

    private val job =
        BalanceReconciliationJob(
            reconciliationService,
            vaultRepository,
            walletAssetRepository,
            internalBalanceRepository,
            reconciliationResultRepository,
            fireblocksBalancePort,
            auditLogRepository,
            breakEventPublisher,
            properties,
        )

    @Test
    fun `should iterate all active vaults and their wallet assets`() {
        // given
        val vault = aVault(fireblocksVaultId = "fb-vault-1")
        val asset = aWalletAsset(vaultId = vault.id)
        val internalBalance = anInternalBalance(vaultId = vault.id.value, currency = asset.currency, protocol = asset.protocol)
        val balanceResult = aBalanceResult(BigDecimal("1000.00"))

        every { vaultRepository.findAllActive() } returns listOf(vault)
        every { walletAssetRepository.findByVaultId(vault.id) } returns listOf(asset)
        every {
            internalBalanceRepository.findByVaultIdAndCurrencyAndProtocol(vault.id.value, asset.currency, asset.protocol)
        } returns internalBalance
        every { fireblocksBalancePort.getBalance("fb-vault-1", asset.fireblocksAssetId, true) } returns balanceResult
        every { reconciliationResultRepository.save(any()) } returnsArgument 0
        every { auditLogRepository.save(any()) } returnsArgument 0

        // when
        job.reconcileBalances()

        // then
        verify { vaultRepository.findAllActive() }
        verify { walletAssetRepository.findByVaultId(vault.id) }
        verify { reconciliationResultRepository.save(any()) }
    }

    @Test
    fun `should seed internal balance from fireblocks on first encounter`() {
        // given
        val vault = aVault(fireblocksVaultId = "fb-vault-seed")
        val asset = aWalletAsset(vaultId = vault.id)
        val balanceResult = aBalanceResult(BigDecimal("500.00"))

        every { vaultRepository.findAllActive() } returns listOf(vault)
        every { walletAssetRepository.findByVaultId(vault.id) } returns listOf(asset)
        every {
            internalBalanceRepository.findByVaultIdAndCurrencyAndProtocol(vault.id.value, asset.currency, asset.protocol)
        } returns null
        every { fireblocksBalancePort.getBalance("fb-vault-seed", asset.fireblocksAssetId, true) } returns balanceResult
        every { internalBalanceRepository.save(any()) } returnsArgument 0
        every { reconciliationResultRepository.save(any()) } returnsArgument 0
        every { auditLogRepository.save(any()) } returnsArgument 0

        // when
        job.reconcileBalances()

        // then
        verify {
            internalBalanceRepository.save(
                match { it.balance == BigDecimal("500.00") && it.vaultId == vault.id.value },
            )
        }
    }

    @Test
    fun `should persist PARTIAL result when fireblocks API fails`() {
        // given
        val vault = aVault(fireblocksVaultId = "fb-vault-fail")
        val asset = aWalletAsset(vaultId = vault.id)
        val internalBalance = anInternalBalance(vaultId = vault.id.value, currency = asset.currency, protocol = asset.protocol)

        every { vaultRepository.findAllActive() } returns listOf(vault)
        every { walletAssetRepository.findByVaultId(vault.id) } returns listOf(asset)
        every {
            internalBalanceRepository.findByVaultIdAndCurrencyAndProtocol(vault.id.value, asset.currency, asset.protocol)
        } returns internalBalance
        every {
            fireblocksBalancePort.getBalance("fb-vault-fail", asset.fireblocksAssetId, true)
        } throws FireblocksApiException("API error")
        every { reconciliationResultRepository.save(any()) } returnsArgument 0
        every { auditLogRepository.save(any()) } returnsArgument 0

        // when
        job.reconcileBalances()

        // then
        verify {
            reconciliationResultRepository.save(
                match { it.status == ReconciliationStatus.PARTIAL },
            )
        }
    }

    @Test
    fun `should publish break event on MISMATCHED`() {
        // given
        val vault = aVault(fireblocksVaultId = "fb-vault-break")
        val asset = aWalletAsset(vaultId = vault.id)
        val internalBalance =
            anInternalBalance(
                vaultId = vault.id.value,
                currency = asset.currency,
                protocol = asset.protocol,
                balance = BigDecimal("1000.00"),
            )
        val balanceResult = aBalanceResult(BigDecimal("900.00"))

        every { vaultRepository.findAllActive() } returns listOf(vault)
        every { walletAssetRepository.findByVaultId(vault.id) } returns listOf(asset)
        every {
            internalBalanceRepository.findByVaultIdAndCurrencyAndProtocol(vault.id.value, asset.currency, asset.protocol)
        } returns internalBalance
        every { fireblocksBalancePort.getBalance("fb-vault-break", asset.fireblocksAssetId, true) } returns balanceResult
        every { reconciliationResultRepository.save(any()) } returnsArgument 0
        every { auditLogRepository.save(any()) } returnsArgument 0
        every { breakEventPublisher.publish(any()) } just runs

        // when
        job.reconcileBalances()

        // then
        verify {
            breakEventPublisher.publish(
                match { it.vaultId == vault.id.value && it.drift.compareTo(BigDecimal("-100.00")) == 0 },
            )
        }
    }

    @Test
    fun `should continue processing remaining pairs when one fails`() {
        // given
        val vault = aVault(fireblocksVaultId = "fb-vault-multi")
        val asset1 = aWalletAsset(vaultId = vault.id, currency = "BTC", protocol = "BTC", fireblocksAssetId = "BTC")
        val asset2 = aWalletAsset(vaultId = vault.id, currency = "ETH", protocol = "ETH", fireblocksAssetId = "ETH")
        val internalBalance2 =
            anInternalBalance(
                vaultId = vault.id.value,
                currency = "ETH",
                protocol = "ETH",
                balance = BigDecimal("100.00"),
            )
        val balanceResult = aBalanceResult(BigDecimal("100.00"))

        every { vaultRepository.findAllActive() } returns listOf(vault)
        every { walletAssetRepository.findByVaultId(vault.id) } returns listOf(asset1, asset2)
        every {
            internalBalanceRepository.findByVaultIdAndCurrencyAndProtocol(vault.id.value, "BTC", "BTC")
        } throws RuntimeException("DB error")
        every {
            internalBalanceRepository.findByVaultIdAndCurrencyAndProtocol(vault.id.value, "ETH", "ETH")
        } returns internalBalance2
        every { fireblocksBalancePort.getBalance("fb-vault-multi", "ETH", true) } returns balanceResult
        every { reconciliationResultRepository.save(any()) } returnsArgument 0
        every { auditLogRepository.save(any()) } returnsArgument 0

        // when
        job.reconcileBalances()

        // then
        verify {
            reconciliationResultRepository.save(
                match { it.currency == "ETH" && it.status == ReconciliationStatus.MATCHED },
            )
        }
    }

    @Test
    fun `should skip when disabled`() {
        // given
        val disabledProperties = ReconciliationProperties(enabled = false)
        val disabledJob =
            BalanceReconciliationJob(
                reconciliationService,
                vaultRepository,
                walletAssetRepository,
                internalBalanceRepository,
                reconciliationResultRepository,
                fireblocksBalancePort,
                auditLogRepository,
                breakEventPublisher,
                disabledProperties,
            )

        // when
        disabledJob.reconcileBalances()

        // then
        verify(exactly = 0) { vaultRepository.findAllActive() }
    }

    private fun aBalanceResult(available: BigDecimal) =
        BalanceResult(
            total = available,
            available = available,
            pending = BigDecimal.ZERO,
            frozen = BigDecimal.ZERO,
            locked = BigDecimal.ZERO,
        )
}
