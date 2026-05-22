package com.stablecoin.custody.fireblocks.infrastructure.scheduling

import com.stablecoin.custody.fireblocks.domain.exception.FireblocksApiException
import com.stablecoin.custody.fireblocks.domain.port.BalanceResult
import com.stablecoin.custody.fireblocks.domain.port.FireblocksBalancePort
import com.stablecoin.custody.fireblocks.domain.reconciliation.BalanceReconciliationService
import com.stablecoin.custody.fireblocks.domain.reconciliation.InternalBalanceRepository
import com.stablecoin.custody.fireblocks.domain.reconciliation.ReconciliationResult
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

private val BTC_TOLERANCE = BigDecimal("0.00001")
private val DEFAULT_TOLERANCE = BigDecimal("0.01")

@ExtendWith(MockKExtension::class)
class BalanceReconciliationJobTest {
    private val reconciliationService: BalanceReconciliationService = mockk()
    private val vaultRepository: VaultRepository = mockk()
    private val walletAssetRepository: WalletAssetRepository = mockk()
    private val internalBalanceRepository: InternalBalanceRepository = mockk()
    private val fireblocksBalancePort: FireblocksBalancePort = mockk()
    private val properties =
        ReconciliationProperties(
            enabled = true,
            interval = 900000,
            defaultTolerance = DEFAULT_TOLERANCE,
            tolerances = mapOf("BTC" to BTC_TOLERANCE),
        )

    private val job =
        BalanceReconciliationJob(
            reconciliationService,
            vaultRepository,
            walletAssetRepository,
            internalBalanceRepository,
            fireblocksBalancePort,
            properties,
        )

    @Test
    fun `should iterate all active vaults and their wallet assets`() {
        // given
        val vault = aVault(fireblocksVaultId = "fb-vault-1")
        val asset = aWalletAsset(vaultId = vault.id)
        val internalBalance = anInternalBalance(vaultId = vault.id.value, currency = asset.currency, protocol = asset.protocol)
        val balanceResult = aBalanceResult(BigDecimal("1000.00"))
        val matchedResult = mockk<ReconciliationResult>()

        every { vaultRepository.findAllActive() } returns listOf(vault)
        every { walletAssetRepository.findByVaultId(vault.id) } returns listOf(asset)
        every {
            internalBalanceRepository.findByVaultIdAndCurrencyAndProtocol(vault.id.value, asset.currency, asset.protocol)
        } returns internalBalance
        every { fireblocksBalancePort.getBalance("fb-vault-1", asset.fireblocksAssetId, true) } returns balanceResult
        every { reconciliationService.reconcile(internalBalance, balanceResult.available, BTC_TOLERANCE) } returns matchedResult
        every { reconciliationService.persistResult(matchedResult) } just runs

        // when
        job.reconcileBalances()

        // then
        verify { vaultRepository.findAllActive() }
        verify { walletAssetRepository.findByVaultId(vault.id) }
        verify { reconciliationService.persistResult(matchedResult) }
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
        every {
            reconciliationService.seedAndPersist(vault.id.value, asset.currency, asset.protocol, BigDecimal("500.00"), BTC_TOLERANCE)
        } just runs

        // when
        job.reconcileBalances()

        // then
        verify {
            reconciliationService.seedAndPersist(
                vault.id.value,
                asset.currency,
                asset.protocol,
                BigDecimal("500.00"),
                BTC_TOLERANCE,
            )
        }
    }

    @Test
    fun `should persist PARTIAL result when fireblocks API fails`() {
        // given
        val vault = aVault(fireblocksVaultId = "fb-vault-fail")
        val asset = aWalletAsset(vaultId = vault.id)
        val internalBalance = anInternalBalance(vaultId = vault.id.value, currency = asset.currency, protocol = asset.protocol)
        val partialResult = mockk<ReconciliationResult>()

        every { vaultRepository.findAllActive() } returns listOf(vault)
        every { walletAssetRepository.findByVaultId(vault.id) } returns listOf(asset)
        every {
            internalBalanceRepository.findByVaultIdAndCurrencyAndProtocol(vault.id.value, asset.currency, asset.protocol)
        } returns internalBalance
        every {
            fireblocksBalancePort.getBalance("fb-vault-fail", asset.fireblocksAssetId, true)
        } throws FireblocksApiException("API error")
        every {
            reconciliationService.createPartialResult(
                vault.id.value,
                asset.currency,
                asset.protocol,
                internalBalance.balance,
                BTC_TOLERANCE,
            )
        } returns partialResult
        every { reconciliationService.persistResult(partialResult) } just runs

        // when
        job.reconcileBalances()

        // then
        verify {
            reconciliationService.createPartialResult(
                vault.id.value,
                asset.currency,
                asset.protocol,
                internalBalance.balance,
                BTC_TOLERANCE,
            )
        }
        verify { reconciliationService.persistResult(partialResult) }
    }

    @Test
    fun `should delegate persistence to service for MISMATCHED result`() {
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
        val mismatchedResult = mockk<ReconciliationResult>()

        every { vaultRepository.findAllActive() } returns listOf(vault)
        every { walletAssetRepository.findByVaultId(vault.id) } returns listOf(asset)
        every {
            internalBalanceRepository.findByVaultIdAndCurrencyAndProtocol(vault.id.value, asset.currency, asset.protocol)
        } returns internalBalance
        every { fireblocksBalancePort.getBalance("fb-vault-break", asset.fireblocksAssetId, true) } returns balanceResult
        every {
            reconciliationService.reconcile(internalBalance, balanceResult.available, BTC_TOLERANCE)
        } returns mismatchedResult
        every { reconciliationService.persistResult(mismatchedResult) } just runs

        // when
        job.reconcileBalances()

        // then
        verify { reconciliationService.persistResult(mismatchedResult) }
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
        val matchedResult = mockk<ReconciliationResult>()

        every { vaultRepository.findAllActive() } returns listOf(vault)
        every { walletAssetRepository.findByVaultId(vault.id) } returns listOf(asset1, asset2)
        every {
            internalBalanceRepository.findByVaultIdAndCurrencyAndProtocol(vault.id.value, "BTC", "BTC")
        } throws RuntimeException("DB error")
        every {
            internalBalanceRepository.findByVaultIdAndCurrencyAndProtocol(vault.id.value, "ETH", "ETH")
        } returns internalBalance2
        every { fireblocksBalancePort.getBalance("fb-vault-multi", "ETH", true) } returns balanceResult
        every { reconciliationService.reconcile(internalBalance2, balanceResult.available, DEFAULT_TOLERANCE) } returns matchedResult
        every { reconciliationService.persistResult(matchedResult) } just runs

        // when
        job.reconcileBalances()

        // then
        verify { reconciliationService.persistResult(matchedResult) }
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
                fireblocksBalancePort,
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
