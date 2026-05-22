package com.stablecoin.custody.fireblocks.domain.transaction

import com.stablecoin.custody.fireblocks.domain.allocation.AllocationStatus
import com.stablecoin.custody.fireblocks.domain.allocation.FundAllocationService
import com.stablecoin.custody.fireblocks.domain.audit.AuditLogRepository
import com.stablecoin.custody.fireblocks.domain.audit.AuditOperation
import com.stablecoin.custody.fireblocks.domain.audit.AuditStatus
import com.stablecoin.custody.fireblocks.domain.event.TransactionStatusChangedEvent
import com.stablecoin.custody.fireblocks.domain.exception.AssetNotFoundException
import com.stablecoin.custody.fireblocks.domain.exception.VaultNotActiveException
import com.stablecoin.custody.fireblocks.domain.exception.VaultNotFoundException
import com.stablecoin.custody.fireblocks.domain.port.EventPublisher
import com.stablecoin.custody.fireblocks.domain.port.FireblocksTransactionPort
import com.stablecoin.custody.fireblocks.domain.vault.VaultId
import com.stablecoin.custody.fireblocks.domain.vault.VaultQueryService
import com.stablecoin.custody.fireblocks.domain.vault.VaultStatus
import com.stablecoin.custody.fireblocks.domain.wallet.SupportedAssetRepository
import com.stablecoin.custody.fireblocks.test.fixtures.aFundAllocation
import com.stablecoin.custody.fireblocks.test.fixtures.aSubmitTransactionCommand
import com.stablecoin.custody.fireblocks.test.fixtures.aSupportedAsset
import com.stablecoin.custody.fireblocks.test.fixtures.aTransaction
import com.stablecoin.custody.fireblocks.test.fixtures.aTransactionResult
import com.stablecoin.custody.fireblocks.test.fixtures.aVault
import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import io.mockk.verifyOrder
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.util.UUID

@ExtendWith(MockKExtension::class)
class TransactionSubmissionHandlerTest {
    private val transactionRepository: TransactionRepository = mockk()
    private val supportedAssetRepository: SupportedAssetRepository = mockk()
    private val vaultQueryService: VaultQueryService = mockk()
    private val fireblocksClient: FireblocksTransactionPort = mockk()
    private val eventPublisher: EventPublisher<TransactionStatusChangedEvent> = mockk()
    private val auditLogRepository: AuditLogRepository = mockk()
    private val fundAllocationService: FundAllocationService = mockk()

    private val handler =
        TransactionSubmissionHandler(
            transactionRepository,
            supportedAssetRepository,
            vaultQueryService,
            fireblocksClient,
            eventPublisher,
            auditLogRepository,
            fundAllocationService,
        )

    @Test
    fun `should create transaction and submit to Fireblocks`() {
        // given
        val vaultId = UUID.randomUUID()
        val vault = aVault(id = VaultId(vaultId), fireblocksVaultId = "fb-vault-001", status = VaultStatus.ACTIVE)
        val command = aSubmitTransactionCommand(sourceVaultId = vaultId.toString())
        val supportedAsset = aSupportedAsset(currency = command.currency, protocol = command.protocol, fireblocksAssetId = "BTC_TEST")
        val transactionResult = aTransactionResult(id = "fb-tx-001", status = "SUBMITTED")
        val lockedAllocation = aFundAllocation(status = AllocationStatus.LOCKED)

        every { transactionRepository.findByExternalTxId(command.externalTxId) } returns null
        every { vaultQueryService.getVault(VaultId(vaultId)) } returns vault
        every { supportedAssetRepository.findByCurrencyAndProtocol(command.currency, command.protocol) } returns supportedAsset
        every { transactionRepository.save(any()) } returnsArgument 0
        every { fundAllocationService.createAndLock(any(), any(), any(), any(), any(), any(), any()) } returns lockedAllocation
        every { fireblocksClient.submitTransaction(any()) } returns transactionResult
        every { fundAllocationService.consume(any(), any()) } returns lockedAllocation
        every { eventPublisher.publish(any()) } just runs
        every { auditLogRepository.save(any()) } returnsArgument 0

        // when
        val result = handler.handle(command)

        // then
        val expected =
            aTransaction(
                externalTxId = command.externalTxId,
                status = TransactionStatus.SUBMITTED,
                fireblocksTransactionId = "fb-tx-001",
                assetId = "BTC_TEST",
                currency = command.currency,
                protocol = command.protocol,
                amount = command.amount,
                sourceVaultId = command.sourceVaultId,
                destinationAddress = command.destinationAddress,
                feeLevel = FeeLevel.MEDIUM,
            )
        assertThat(result)
            .usingRecursiveComparison()
            .ignoringFields("id", "createdAt", "updatedAt")
            .isEqualTo(expected)
    }

    @Test
    fun `should resolve sourceVaultId to fireblocksVaultId via VaultQueryService`() {
        // given
        val vaultId = UUID.randomUUID()
        val vault = aVault(id = VaultId(vaultId), fireblocksVaultId = "fb-vault-resolved", status = VaultStatus.ACTIVE)
        val command = aSubmitTransactionCommand(sourceVaultId = vaultId.toString())
        val supportedAsset = aSupportedAsset(currency = command.currency, protocol = command.protocol)
        val transactionResult = aTransactionResult()
        val lockedAllocation = aFundAllocation(status = AllocationStatus.LOCKED)

        every { transactionRepository.findByExternalTxId(command.externalTxId) } returns null
        every { vaultQueryService.getVault(VaultId(vaultId)) } returns vault
        every { supportedAssetRepository.findByCurrencyAndProtocol(command.currency, command.protocol) } returns supportedAsset
        every { transactionRepository.save(any()) } returnsArgument 0
        every { fundAllocationService.createAndLock(any(), any(), any(), any(), any(), any(), any()) } returns lockedAllocation
        every { fireblocksClient.submitTransaction(any()) } returns transactionResult
        every { fundAllocationService.consume(any(), any()) } returns lockedAllocation
        every { eventPublisher.publish(any()) } just runs
        every { auditLogRepository.save(any()) } returnsArgument 0

        // when
        handler.handle(command)

        // then
        verify { vaultQueryService.getVault(VaultId(vaultId)) }
    }

    @Test
    fun `should throw VaultNotFoundException for unknown sourceVaultId`() {
        // given
        val vaultId = UUID.randomUUID()
        val command = aSubmitTransactionCommand(sourceVaultId = vaultId.toString())

        every { transactionRepository.findByExternalTxId(command.externalTxId) } returns null
        every { vaultQueryService.getVault(VaultId(vaultId)) } throws VaultNotFoundException(vaultId.toString())

        // when/then
        assertThatThrownBy { handler.handle(command) }
            .isInstanceOf(VaultNotFoundException::class.java)
    }

    @Test
    fun `should throw VaultNotActiveException for PENDING vault`() {
        // given
        val vaultId = UUID.randomUUID()
        val vault = aVault(id = VaultId(vaultId), status = VaultStatus.PENDING, fireblocksVaultId = null)
        val command = aSubmitTransactionCommand(sourceVaultId = vaultId.toString())

        every { transactionRepository.findByExternalTxId(command.externalTxId) } returns null
        every { vaultQueryService.getVault(VaultId(vaultId)) } returns vault

        // when/then
        assertThatThrownBy { handler.handle(command) }
            .isInstanceOf(VaultNotActiveException::class.java)
    }

    @Test
    fun `should use vault fireblocksVaultId in FireblocksSubmitCommand`() {
        // given
        val vaultId = UUID.randomUUID()
        val vault = aVault(id = VaultId(vaultId), fireblocksVaultId = "fb-vault-specific", status = VaultStatus.ACTIVE)
        val command = aSubmitTransactionCommand(sourceVaultId = vaultId.toString())
        val supportedAsset = aSupportedAsset(currency = command.currency, protocol = command.protocol)
        val transactionResult = aTransactionResult()
        val lockedAllocation = aFundAllocation(status = AllocationStatus.LOCKED)

        every { transactionRepository.findByExternalTxId(command.externalTxId) } returns null
        every { vaultQueryService.getVault(VaultId(vaultId)) } returns vault
        every { supportedAssetRepository.findByCurrencyAndProtocol(command.currency, command.protocol) } returns supportedAsset
        every { transactionRepository.save(any()) } returnsArgument 0
        every { fundAllocationService.createAndLock(any(), any(), any(), any(), any(), any(), any()) } returns lockedAllocation
        every { fireblocksClient.submitTransaction(match { it.sourceVaultId == "fb-vault-specific" }) } returns transactionResult
        every { fundAllocationService.consume(any(), any()) } returns lockedAllocation
        every { eventPublisher.publish(any()) } just runs
        every { auditLogRepository.save(any()) } returnsArgument 0

        // when
        handler.handle(command)

        // then
        verify { fireblocksClient.submitTransaction(match { it.sourceVaultId == "fb-vault-specific" }) }
    }

    @Test
    fun `should resolve currency and protocol to fireblocksAssetId via SupportedAssetRepository`() {
        // given
        val vaultId = UUID.randomUUID()
        val vault = aVault(id = VaultId(vaultId), status = VaultStatus.ACTIVE)
        val command = aSubmitTransactionCommand(sourceVaultId = vaultId.toString(), currency = "ETH", protocol = "ETH")
        val supportedAsset = aSupportedAsset(currency = "ETH", protocol = "ETH", fireblocksAssetId = "ETH_TEST5")
        val transactionResult = aTransactionResult()
        val lockedAllocation = aFundAllocation(status = AllocationStatus.LOCKED)

        every { transactionRepository.findByExternalTxId(command.externalTxId) } returns null
        every { vaultQueryService.getVault(VaultId(vaultId)) } returns vault
        every { supportedAssetRepository.findByCurrencyAndProtocol("ETH", "ETH") } returns supportedAsset
        every { transactionRepository.save(any()) } returnsArgument 0
        every { fundAllocationService.createAndLock(any(), any(), any(), any(), any(), any(), any()) } returns lockedAllocation
        every { fireblocksClient.submitTransaction(match { it.assetId == "ETH_TEST5" }) } returns transactionResult
        every { fundAllocationService.consume(any(), any()) } returns lockedAllocation
        every { eventPublisher.publish(any()) } just runs
        every { auditLogRepository.save(any()) } returnsArgument 0

        // when
        handler.handle(command)

        // then
        verify { fireblocksClient.submitTransaction(match { it.assetId == "ETH_TEST5" }) }
    }

    @Test
    fun `should throw AssetNotFoundException for unsupported currency-protocol pair`() {
        // given
        val vaultId = UUID.randomUUID()
        val vault = aVault(id = VaultId(vaultId), status = VaultStatus.ACTIVE)
        val command = aSubmitTransactionCommand(sourceVaultId = vaultId.toString(), currency = "UNKNOWN", protocol = "UNKNOWN")

        every { transactionRepository.findByExternalTxId(command.externalTxId) } returns null
        every { vaultQueryService.getVault(VaultId(vaultId)) } returns vault
        every { supportedAssetRepository.findByCurrencyAndProtocol("UNKNOWN", "UNKNOWN") } returns null

        // when/then
        assertThatThrownBy { handler.handle(command) }
            .isInstanceOf(AssetNotFoundException::class.java)
    }

    @Test
    fun `should return existing transaction for duplicate externalTxId`() {
        // given
        val existingTransaction = aTransaction(status = TransactionStatus.SUBMITTED, fireblocksTransactionId = "fb-existing")
        val command = aSubmitTransactionCommand(externalTxId = existingTransaction.externalTxId)

        every { transactionRepository.findByExternalTxId(existingTransaction.externalTxId) } returns existingTransaction

        // when
        val result = handler.handle(command)

        // then
        assertThat(result).usingRecursiveComparison().isEqualTo(existingTransaction)
        verify(exactly = 0) { fireblocksClient.submitTransaction(any()) }
    }

    @Test
    fun `should mark transaction FAILED and return it when Fireblocks call fails`() {
        // given
        val vaultId = UUID.randomUUID()
        val vault = aVault(id = VaultId(vaultId), status = VaultStatus.ACTIVE)
        val command = aSubmitTransactionCommand(sourceVaultId = vaultId.toString())
        val supportedAsset = aSupportedAsset(currency = command.currency, protocol = command.protocol)
        val lockedAllocation = aFundAllocation(status = AllocationStatus.LOCKED)

        every { transactionRepository.findByExternalTxId(command.externalTxId) } returns null
        every { vaultQueryService.getVault(VaultId(vaultId)) } returns vault
        every { supportedAssetRepository.findByCurrencyAndProtocol(command.currency, command.protocol) } returns supportedAsset
        every { transactionRepository.save(any()) } returnsArgument 0
        every { fundAllocationService.createAndLock(any(), any(), any(), any(), any(), any(), any()) } returns lockedAllocation
        every { fireblocksClient.submitTransaction(any()) } throws RuntimeException("Fireblocks unavailable")
        every { fundAllocationService.release(any()) } returns lockedAllocation
        every { auditLogRepository.save(any()) } returnsArgument 0

        // when
        val result = handler.handle(command)

        // then
        val expected =
            aTransaction(
                externalTxId = command.externalTxId,
                status = TransactionStatus.FAILED,
                assetId = supportedAsset.fireblocksAssetId,
                currency = command.currency,
                protocol = command.protocol,
                amount = command.amount,
                sourceVaultId = command.sourceVaultId,
                destinationAddress = command.destinationAddress,
            )
        assertThat(result)
            .usingRecursiveComparison()
            .ignoringFields("id", "createdAt", "updatedAt")
            .isEqualTo(expected)
    }

    @Test
    fun `should publish TransactionStatusChangedEvent on success`() {
        // given
        val vaultId = UUID.randomUUID()
        val vault = aVault(id = VaultId(vaultId), status = VaultStatus.ACTIVE)
        val command = aSubmitTransactionCommand(sourceVaultId = vaultId.toString())
        val supportedAsset = aSupportedAsset(currency = command.currency, protocol = command.protocol)
        val transactionResult = aTransactionResult(id = "fb-tx-event-001")
        val lockedAllocation = aFundAllocation(status = AllocationStatus.LOCKED)

        every { transactionRepository.findByExternalTxId(command.externalTxId) } returns null
        every { vaultQueryService.getVault(VaultId(vaultId)) } returns vault
        every { supportedAssetRepository.findByCurrencyAndProtocol(command.currency, command.protocol) } returns supportedAsset
        every { transactionRepository.save(any()) } returnsArgument 0
        every { fundAllocationService.createAndLock(any(), any(), any(), any(), any(), any(), any()) } returns lockedAllocation
        every { fireblocksClient.submitTransaction(any()) } returns transactionResult
        every { fundAllocationService.consume(any(), any()) } returns lockedAllocation
        every { eventPublisher.publish(any()) } just runs
        every { auditLogRepository.save(any()) } returnsArgument 0

        // when
        handler.handle(command)

        // then
        verify {
            eventPublisher.publish(
                match {
                    it.previousStatus == "CREATED" &&
                        it.newStatus == "SUBMITTED" &&
                        it.externalTxId == command.externalTxId
                },
            )
        }
    }

    @Test
    fun `should save audit log on success`() {
        // given
        val vaultId = UUID.randomUUID()
        val vault = aVault(id = VaultId(vaultId), status = VaultStatus.ACTIVE)
        val command = aSubmitTransactionCommand(sourceVaultId = vaultId.toString())
        val supportedAsset = aSupportedAsset(currency = command.currency, protocol = command.protocol)
        val transactionResult = aTransactionResult()
        val lockedAllocation = aFundAllocation(status = AllocationStatus.LOCKED)

        every { transactionRepository.findByExternalTxId(command.externalTxId) } returns null
        every { vaultQueryService.getVault(VaultId(vaultId)) } returns vault
        every { supportedAssetRepository.findByCurrencyAndProtocol(command.currency, command.protocol) } returns supportedAsset
        every { transactionRepository.save(any()) } returnsArgument 0
        every { fundAllocationService.createAndLock(any(), any(), any(), any(), any(), any(), any()) } returns lockedAllocation
        every { fireblocksClient.submitTransaction(any()) } returns transactionResult
        every { fundAllocationService.consume(any(), any()) } returns lockedAllocation
        every { eventPublisher.publish(any()) } just runs
        every { auditLogRepository.save(any()) } returnsArgument 0

        // when
        handler.handle(command)

        // then
        verify {
            auditLogRepository.save(
                match {
                    it.operation == AuditOperation.TRANSACTION_SUBMITTED &&
                        it.status == AuditStatus.SUCCESS
                },
            )
        }
    }

    @Test
    fun `should save failure audit log when Fireblocks fails`() {
        // given
        val vaultId = UUID.randomUUID()
        val vault = aVault(id = VaultId(vaultId), status = VaultStatus.ACTIVE)
        val command = aSubmitTransactionCommand(sourceVaultId = vaultId.toString())
        val supportedAsset = aSupportedAsset(currency = command.currency, protocol = command.protocol)
        val lockedAllocation = aFundAllocation(status = AllocationStatus.LOCKED)

        every { transactionRepository.findByExternalTxId(command.externalTxId) } returns null
        every { vaultQueryService.getVault(VaultId(vaultId)) } returns vault
        every { supportedAssetRepository.findByCurrencyAndProtocol(command.currency, command.protocol) } returns supportedAsset
        every { transactionRepository.save(any()) } returnsArgument 0
        every { fundAllocationService.createAndLock(any(), any(), any(), any(), any(), any(), any()) } returns lockedAllocation
        every { fireblocksClient.submitTransaction(any()) } throws RuntimeException("Fireblocks error")
        every { fundAllocationService.release(any()) } returns lockedAllocation
        every { auditLogRepository.save(any()) } returnsArgument 0

        // when
        handler.handle(command)

        // then
        verify {
            auditLogRepository.save(
                match {
                    it.operation == AuditOperation.TRANSACTION_SUBMISSION_FAILED &&
                        it.status == AuditStatus.FAILURE
                },
            )
        }
    }

    @Test
    fun `should NOT throw exception when Fireblocks fails`() {
        // given
        val vaultId = UUID.randomUUID()
        val vault = aVault(id = VaultId(vaultId), status = VaultStatus.ACTIVE)
        val command = aSubmitTransactionCommand(sourceVaultId = vaultId.toString())
        val supportedAsset = aSupportedAsset(currency = command.currency, protocol = command.protocol)
        val lockedAllocation = aFundAllocation(status = AllocationStatus.LOCKED)

        every { transactionRepository.findByExternalTxId(command.externalTxId) } returns null
        every { vaultQueryService.getVault(VaultId(vaultId)) } returns vault
        every { supportedAssetRepository.findByCurrencyAndProtocol(command.currency, command.protocol) } returns supportedAsset
        every { transactionRepository.save(any()) } returnsArgument 0
        every { fundAllocationService.createAndLock(any(), any(), any(), any(), any(), any(), any()) } returns lockedAllocation
        every { fireblocksClient.submitTransaction(any()) } throws RuntimeException("Fireblocks error")
        every { fundAllocationService.release(any()) } returns lockedAllocation
        every { auditLogRepository.save(any()) } returnsArgument 0

        // when
        val result = handler.handle(command)

        // then
        assertThat(result.status).isEqualTo(TransactionStatus.FAILED)
    }

    @Test
    fun `should use correct fee level from command`() {
        // given
        val vaultId = UUID.randomUUID()
        val vault = aVault(id = VaultId(vaultId), status = VaultStatus.ACTIVE)
        val command = aSubmitTransactionCommand(sourceVaultId = vaultId.toString(), feeLevel = FeeLevel.HIGH)
        val supportedAsset = aSupportedAsset(currency = command.currency, protocol = command.protocol)
        val transactionResult = aTransactionResult()
        val lockedAllocation = aFundAllocation(status = AllocationStatus.LOCKED)

        every { transactionRepository.findByExternalTxId(command.externalTxId) } returns null
        every { vaultQueryService.getVault(VaultId(vaultId)) } returns vault
        every { supportedAssetRepository.findByCurrencyAndProtocol(command.currency, command.protocol) } returns supportedAsset
        every { transactionRepository.save(any()) } returnsArgument 0
        every { fundAllocationService.createAndLock(any(), any(), any(), any(), any(), any(), any()) } returns lockedAllocation
        every { fireblocksClient.submitTransaction(match { it.feeLevel == FeeLevel.HIGH }) } returns transactionResult
        every { fundAllocationService.consume(any(), any()) } returns lockedAllocation
        every { eventPublisher.publish(any()) } just runs
        every { auditLogRepository.save(any()) } returnsArgument 0

        // when
        handler.handle(command)

        // then
        verify { fireblocksClient.submitTransaction(match { it.feeLevel == FeeLevel.HIGH }) }
    }

    @Test
    fun `should default fee level to MEDIUM when not specified`() {
        // given
        val vaultId = UUID.randomUUID()
        val vault = aVault(id = VaultId(vaultId), status = VaultStatus.ACTIVE)
        val command = aSubmitTransactionCommand(sourceVaultId = vaultId.toString(), feeLevel = null)
        val supportedAsset = aSupportedAsset(currency = command.currency, protocol = command.protocol)
        val transactionResult = aTransactionResult()
        val lockedAllocation = aFundAllocation(status = AllocationStatus.LOCKED)

        every { transactionRepository.findByExternalTxId(command.externalTxId) } returns null
        every { vaultQueryService.getVault(VaultId(vaultId)) } returns vault
        every { supportedAssetRepository.findByCurrencyAndProtocol(command.currency, command.protocol) } returns supportedAsset
        every { transactionRepository.save(any()) } returnsArgument 0
        every { fundAllocationService.createAndLock(any(), any(), any(), any(), any(), any(), any()) } returns lockedAllocation
        every { fireblocksClient.submitTransaction(match { it.feeLevel == FeeLevel.MEDIUM }) } returns transactionResult
        every { fundAllocationService.consume(any(), any()) } returns lockedAllocation
        every { eventPublisher.publish(any()) } just runs
        every { auditLogRepository.save(any()) } returnsArgument 0

        // when
        handler.handle(command)

        // then
        verify { fireblocksClient.submitTransaction(match { it.feeLevel == FeeLevel.MEDIUM }) }
    }

    @Test
    fun `should create allocation and lock before submission`() {
        // given
        val vaultId = UUID.randomUUID()
        val vault = aVault(id = VaultId(vaultId), fireblocksVaultId = "fb-vault-001", status = VaultStatus.ACTIVE)
        val command = aSubmitTransactionCommand(sourceVaultId = vaultId.toString())
        val supportedAsset = aSupportedAsset(currency = command.currency, protocol = command.protocol)
        val transactionResult = aTransactionResult()
        val lockedAllocation = aFundAllocation(status = AllocationStatus.LOCKED, fireblocksVaultId = "fb-vault-001")

        every { transactionRepository.findByExternalTxId(command.externalTxId) } returns null
        every { vaultQueryService.getVault(VaultId(vaultId)) } returns vault
        every { supportedAssetRepository.findByCurrencyAndProtocol(command.currency, command.protocol) } returns supportedAsset
        every { transactionRepository.save(any()) } returnsArgument 0
        every { fundAllocationService.createAndLock(any(), any(), any(), any(), any(), any(), any()) } returns lockedAllocation
        every { fireblocksClient.submitTransaction(any()) } returns transactionResult
        every { fundAllocationService.consume(any(), any()) } returns lockedAllocation
        every { eventPublisher.publish(any()) } just runs
        every { auditLogRepository.save(any()) } returnsArgument 0

        // when
        handler.handle(command)

        // then
        verifyOrder {
            fundAllocationService.createAndLock(
                match { it == "alloc-${command.externalTxId}" },
                any(),
                match { it == "fb-vault-001" },
                any(),
                any(),
                any(),
                any(),
            )
            fireblocksClient.submitTransaction(any())
        }
    }

    @Test
    fun `should mark transaction FAILED when allocation lock fails`() {
        // given
        val vaultId = UUID.randomUUID()
        val vault = aVault(id = VaultId(vaultId), status = VaultStatus.ACTIVE)
        val command = aSubmitTransactionCommand(sourceVaultId = vaultId.toString())
        val supportedAsset = aSupportedAsset(currency = command.currency, protocol = command.protocol)
        val failedAllocation = aFundAllocation(status = AllocationStatus.FAILED)

        every { transactionRepository.findByExternalTxId(command.externalTxId) } returns null
        every { vaultQueryService.getVault(VaultId(vaultId)) } returns vault
        every { supportedAssetRepository.findByCurrencyAndProtocol(command.currency, command.protocol) } returns supportedAsset
        every { transactionRepository.save(any()) } returnsArgument 0
        every { fundAllocationService.createAndLock(any(), any(), any(), any(), any(), any(), any()) } returns failedAllocation
        every { auditLogRepository.save(any()) } returnsArgument 0

        // when
        val result = handler.handle(command)

        // then
        assertThat(result.status).isEqualTo(TransactionStatus.FAILED)
        verify(exactly = 0) { fireblocksClient.submitTransaction(any()) }
    }

    @Test
    fun `should release allocation when transaction submission fails`() {
        // given
        val vaultId = UUID.randomUUID()
        val vault = aVault(id = VaultId(vaultId), status = VaultStatus.ACTIVE)
        val command = aSubmitTransactionCommand(sourceVaultId = vaultId.toString())
        val supportedAsset = aSupportedAsset(currency = command.currency, protocol = command.protocol)
        val lockedAllocation = aFundAllocation(status = AllocationStatus.LOCKED)

        every { transactionRepository.findByExternalTxId(command.externalTxId) } returns null
        every { vaultQueryService.getVault(VaultId(vaultId)) } returns vault
        every { supportedAssetRepository.findByCurrencyAndProtocol(command.currency, command.protocol) } returns supportedAsset
        every { transactionRepository.save(any()) } returnsArgument 0
        every { fundAllocationService.createAndLock(any(), any(), any(), any(), any(), any(), any()) } returns lockedAllocation
        every { fireblocksClient.submitTransaction(any()) } throws RuntimeException("Fireblocks down")
        every { fundAllocationService.release(lockedAllocation.allocationId) } returns lockedAllocation
        every { auditLogRepository.save(any()) } returnsArgument 0

        // when
        handler.handle(command)

        // then
        verify { fundAllocationService.release(lockedAllocation.allocationId) }
    }

    @Test
    fun `should consume allocation after successful submission`() {
        // given
        val vaultId = UUID.randomUUID()
        val vault = aVault(id = VaultId(vaultId), status = VaultStatus.ACTIVE)
        val command = aSubmitTransactionCommand(sourceVaultId = vaultId.toString())
        val supportedAsset = aSupportedAsset(currency = command.currency, protocol = command.protocol)
        val transactionResult = aTransactionResult()
        val lockedAllocation = aFundAllocation(status = AllocationStatus.LOCKED)

        every { transactionRepository.findByExternalTxId(command.externalTxId) } returns null
        every { vaultQueryService.getVault(VaultId(vaultId)) } returns vault
        every { supportedAssetRepository.findByCurrencyAndProtocol(command.currency, command.protocol) } returns supportedAsset
        every { transactionRepository.save(any()) } returnsArgument 0
        every { fundAllocationService.createAndLock(any(), any(), any(), any(), any(), any(), any()) } returns lockedAllocation
        every { fireblocksClient.submitTransaction(any()) } returns transactionResult
        every { fundAllocationService.consume(any(), any()) } returns lockedAllocation
        every { eventPublisher.publish(any()) } just runs
        every { auditLogRepository.save(any()) } returnsArgument 0

        // when
        handler.handle(command)

        // then
        verify { fundAllocationService.consume(lockedAllocation.allocationId, any()) }
    }
}
