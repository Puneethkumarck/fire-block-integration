package com.stablecoin.custody.fireblocks.application.controller

import com.stablecoin.custody.fireblocks.application.exception.GlobalExceptionHandler
import com.stablecoin.custody.fireblocks.domain.exception.TransactionAlreadyTerminalException
import com.stablecoin.custody.fireblocks.domain.exception.TransactionNotCancellableException
import com.stablecoin.custody.fireblocks.domain.exception.TransactionNotFoundException
import com.stablecoin.custody.fireblocks.domain.exception.VaultNotFoundException
import com.stablecoin.custody.fireblocks.domain.port.FeeEstimateResult
import com.stablecoin.custody.fireblocks.domain.port.FireblocksEstimateFeeCommand
import com.stablecoin.custody.fireblocks.domain.port.FireblocksTransactionPort
import com.stablecoin.custody.fireblocks.domain.transaction.SubmitTransactionCommand
import com.stablecoin.custody.fireblocks.domain.transaction.TransactionCancellationHandler
import com.stablecoin.custody.fireblocks.domain.transaction.TransactionId
import com.stablecoin.custody.fireblocks.domain.transaction.TransactionQueryService
import com.stablecoin.custody.fireblocks.domain.transaction.TransactionStatus
import com.stablecoin.custody.fireblocks.domain.transaction.TransactionSubmissionHandler
import com.stablecoin.custody.fireblocks.domain.vault.VaultId
import com.stablecoin.custody.fireblocks.domain.vault.VaultQueryService
import com.stablecoin.custody.fireblocks.domain.vault.VaultStatus
import com.stablecoin.custody.fireblocks.domain.wallet.SupportedAssetRepository
import com.stablecoin.custody.fireblocks.test.fixtures.aSupportedAsset
import com.stablecoin.custody.fireblocks.test.fixtures.aTransaction
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
import java.math.BigDecimal
import java.util.UUID

class TransactionControllerTest {
    private val transactionSubmissionHandler: TransactionSubmissionHandler = mockk()
    private val transactionCancellationHandler: TransactionCancellationHandler = mockk()
    private val transactionQueryService: TransactionQueryService = mockk()
    private val vaultQueryService: VaultQueryService = mockk()
    private val supportedAssetRepository: SupportedAssetRepository = mockk()
    private val fireblocksTransactionPort: FireblocksTransactionPort = mockk()
    private val controller =
        TransactionController(
            transactionSubmissionHandler,
            transactionCancellationHandler,
            transactionQueryService,
            vaultQueryService,
            supportedAssetRepository,
            fireblocksTransactionPort,
        )
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
    fun `should submit transaction and return 201`() {
        // given
        val sourceVaultId = UUID.randomUUID().toString()
        val transaction = aTransaction(sourceVaultId = sourceVaultId)
        val command =
            SubmitTransactionCommand(
                externalTxId = "ext-tx-001",
                sourceVaultId = sourceVaultId,
                destinationAddress = "0xabc123",
                currency = "BTC",
                protocol = "BTC",
                amount = BigDecimal("1.5"),
                feeLevel = null,
                treatAsGrossAmount = null,
                note = null,
            )
        every { transactionSubmissionHandler.handle(command) } returns transaction

        // when / then
        mockMvc
            .perform(
                post("/api/v1/transactions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """{"externalTxId":"ext-tx-001","sourceVaultId":"$sourceVaultId","destinationAddress":"0xabc123","currency":"BTC","protocol":"BTC","amount":1.5}""",
                    ),
            ).andExpect(status().isCreated)
            .andExpect(jsonPath("$.externalTxId").value(transaction.externalTxId))
            .andExpect(jsonPath("$.status").value(transaction.status.name))
            .andExpect(jsonPath("$.currency").value(transaction.currency))
            .andExpect(jsonPath("$.protocol").value(transaction.protocol))
    }

    @Test
    fun `should return existing transaction for duplicate externalTxId`() {
        // given
        val transaction = aTransaction(externalTxId = "ext-tx-dup")
        val command =
            SubmitTransactionCommand(
                externalTxId = "ext-tx-dup",
                sourceVaultId = transaction.sourceVaultId,
                destinationAddress = "0xabc123",
                currency = "BTC",
                protocol = "BTC",
                amount = BigDecimal("1.5"),
                feeLevel = null,
                treatAsGrossAmount = null,
                note = null,
            )
        every { transactionSubmissionHandler.handle(command) } returns transaction

        // when / then
        mockMvc
            .perform(
                post("/api/v1/transactions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """{"externalTxId":"ext-tx-dup","sourceVaultId":"${transaction.sourceVaultId}","destinationAddress":"0xabc123","currency":"BTC","protocol":"BTC","amount":1.5}""",
                    ),
            ).andExpect(status().isCreated)
            .andExpect(jsonPath("$.externalTxId").value("ext-tx-dup"))
    }

    @Test
    fun `should get transaction by externalTxId`() {
        // given
        val transaction = aTransaction(externalTxId = "ext-tx-get")
        every { transactionQueryService.getByExternalTxId("ext-tx-get") } returns transaction

        // when / then
        mockMvc
            .perform(get("/api/v1/transactions/ext-tx-get"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.externalTxId").value("ext-tx-get"))
            .andExpect(jsonPath("$.id").value(transaction.id.value.toString()))
    }

    @Test
    fun `should get transaction by fireblocksTxId`() {
        // given
        val transaction = aTransaction(fireblocksTransactionId = "fb-tx-001")
        every { transactionQueryService.getByFireblocksTxId("fb-tx-001") } returns transaction

        // when / then
        mockMvc
            .perform(get("/api/v1/transactions/fireblocks/fb-tx-001"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.fireblocksTransactionId").value("fb-tx-001"))
    }

    @Test
    fun `should return 404 for non-existent transaction`() {
        // given
        every { transactionQueryService.getByExternalTxId("not-found") } throws
            TransactionNotFoundException("not-found")

        // when / then
        mockMvc
            .perform(get("/api/v1/transactions/not-found"))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `should return 400 for invalid request body`() {
        // when / then
        mockMvc
            .perform(
                post("/api/v1/transactions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"externalTxId":"","sourceVaultId":"","destinationAddress":"","currency":"","protocol":"","amount":0}"""),
            ).andExpect(status().isBadRequest)
    }

    @Test
    fun `should return 400 for blank currency`() {
        // when / then
        mockMvc
            .perform(
                post("/api/v1/transactions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """{"externalTxId":"ext-001","sourceVaultId":"${UUID.randomUUID()}","destinationAddress":"0xabc123","currency":"","protocol":"BTC","amount":1.5}""",
                    ),
            ).andExpect(status().isBadRequest)
    }

    @Test
    fun `should return 400 for blank protocol`() {
        // when / then
        mockMvc
            .perform(
                post("/api/v1/transactions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """{"externalTxId":"ext-001","sourceVaultId":"${UUID.randomUUID()}","destinationAddress":"0xabc123","currency":"BTC","protocol":"","amount":1.5}""",
                    ),
            ).andExpect(status().isBadRequest)
    }

    @Test
    fun `should return 400 for zero amount`() {
        // when / then
        mockMvc
            .perform(
                post("/api/v1/transactions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """{"externalTxId":"ext-001","sourceVaultId":"${UUID.randomUUID()}","destinationAddress":"0xabc123","currency":"BTC","protocol":"BTC","amount":0}""",
                    ),
            ).andExpect(status().isBadRequest)
    }

    @Test
    fun `should estimate fee and return all levels`() {
        // given
        val vaultId = UUID.randomUUID()
        val vault = aVault(id = VaultId(vaultId), fireblocksVaultId = "fb-vault-001")
        val supportedAsset = aSupportedAsset(fireblocksAssetId = "BTC_TEST")
        val feeResult =
            FeeEstimateResult(
                low = BigDecimal("0.0001"),
                medium = BigDecimal("0.0005"),
                high = BigDecimal("0.001"),
            )
        every { vaultQueryService.getVault(VaultId(vaultId)) } returns vault
        every { supportedAssetRepository.findByCurrencyAndProtocol("BTC", "BTC") } returns supportedAsset
        every {
            fireblocksTransactionPort.estimateFee(
                FireblocksEstimateFeeCommand(
                    sourceVaultId = "fb-vault-001",
                    destinationAddress = "0xabc123",
                    assetId = "BTC_TEST",
                    amount = BigDecimal("1.5"),
                ),
            )
        } returns feeResult

        // when / then
        mockMvc
            .perform(
                post("/api/v1/transactions/estimate-fee")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """{"sourceVaultId":"$vaultId","destinationAddress":"0xabc123","currency":"BTC","protocol":"BTC","amount":1.5}""",
                    ),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.low").value(0.0001))
            .andExpect(jsonPath("$.medium").value(0.0005))
            .andExpect(jsonPath("$.high").value(0.001))
    }

    @Test
    fun `should resolve sourceVaultId to fireblocksVaultId for fee estimation`() {
        // given
        val vaultId = UUID.randomUUID()
        val vault = aVault(id = VaultId(vaultId), fireblocksVaultId = "fb-resolved-001")
        val supportedAsset = aSupportedAsset(fireblocksAssetId = "BTC")
        val feeResult = FeeEstimateResult(BigDecimal("0.001"), BigDecimal("0.002"), BigDecimal("0.003"))
        every { vaultQueryService.getVault(VaultId(vaultId)) } returns vault
        every { supportedAssetRepository.findByCurrencyAndProtocol("BTC", "BTC") } returns supportedAsset
        every {
            fireblocksTransactionPort.estimateFee(
                FireblocksEstimateFeeCommand(
                    sourceVaultId = "fb-resolved-001",
                    destinationAddress = "0xdest",
                    assetId = "BTC",
                    amount = BigDecimal("2.0"),
                ),
            )
        } returns feeResult

        // when / then
        mockMvc
            .perform(
                post("/api/v1/transactions/estimate-fee")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """{"sourceVaultId":"$vaultId","destinationAddress":"0xdest","currency":"BTC","protocol":"BTC","amount":2.0}""",
                    ),
            ).andExpect(status().isOk)

        verify {
            fireblocksTransactionPort.estimateFee(
                FireblocksEstimateFeeCommand(
                    sourceVaultId = "fb-resolved-001",
                    destinationAddress = "0xdest",
                    assetId = "BTC",
                    amount = BigDecimal("2.0"),
                ),
            )
        }
    }

    @Test
    fun `should resolve currency and protocol for fee estimation`() {
        // given
        val vaultId = UUID.randomUUID()
        val vault = aVault(id = VaultId(vaultId), fireblocksVaultId = "fb-001")
        val supportedAsset = aSupportedAsset(currency = "ETH", protocol = "ETH", fireblocksAssetId = "ETH_TEST")
        val feeResult = FeeEstimateResult(BigDecimal("0.01"), BigDecimal("0.02"), BigDecimal("0.03"))
        every { vaultQueryService.getVault(VaultId(vaultId)) } returns vault
        every { supportedAssetRepository.findByCurrencyAndProtocol("ETH", "ETH") } returns supportedAsset
        every {
            fireblocksTransactionPort.estimateFee(
                FireblocksEstimateFeeCommand(
                    sourceVaultId = "fb-001",
                    destinationAddress = "0xethaddr",
                    assetId = "ETH_TEST",
                    amount = BigDecimal("5.0"),
                ),
            )
        } returns feeResult

        // when / then
        mockMvc
            .perform(
                post("/api/v1/transactions/estimate-fee")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """{"sourceVaultId":"$vaultId","destinationAddress":"0xethaddr","currency":"ETH","protocol":"ETH","amount":5.0}""",
                    ),
            ).andExpect(status().isOk)

        verify {
            fireblocksTransactionPort.estimateFee(
                FireblocksEstimateFeeCommand(
                    sourceVaultId = "fb-001",
                    destinationAddress = "0xethaddr",
                    assetId = "ETH_TEST",
                    amount = BigDecimal("5.0"),
                ),
            )
        }
    }

    @Test
    fun `should return 404 for unknown sourceVaultId in fee estimation`() {
        // given
        val vaultId = UUID.randomUUID()
        every { vaultQueryService.getVault(VaultId(vaultId)) } throws VaultNotFoundException(vaultId.toString())

        // when / then
        mockMvc
            .perform(
                post("/api/v1/transactions/estimate-fee")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """{"sourceVaultId":"$vaultId","destinationAddress":"0xabc","currency":"BTC","protocol":"BTC","amount":1.0}""",
                    ),
            ).andExpect(status().isNotFound)
    }

    @Test
    fun `should return 422 for inactive vault in fee estimation`() {
        // given
        val vaultId = UUID.randomUUID()
        val vault = aVault(id = VaultId(vaultId), status = VaultStatus.PENDING)
        every { vaultQueryService.getVault(VaultId(vaultId)) } returns vault

        // when / then
        mockMvc
            .perform(
                post("/api/v1/transactions/estimate-fee")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """{"sourceVaultId":"$vaultId","destinationAddress":"0xabc","currency":"BTC","protocol":"BTC","amount":1.0}""",
                    ),
            ).andExpect(status().isUnprocessableEntity)
    }

    @Test
    fun `should return 404 for unsupported currency-protocol pair`() {
        // given
        val vaultId = UUID.randomUUID()
        val vault = aVault(id = VaultId(vaultId), fireblocksVaultId = "fb-001")
        every { vaultQueryService.getVault(VaultId(vaultId)) } returns vault
        every { supportedAssetRepository.findByCurrencyAndProtocol("FAKE", "FAKE") } returns null

        // when / then
        mockMvc
            .perform(
                post("/api/v1/transactions/estimate-fee")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """{"sourceVaultId":"$vaultId","destinationAddress":"0xabc","currency":"FAKE","protocol":"FAKE","amount":1.0}""",
                    ),
            ).andExpect(status().isNotFound)
    }

    @Test
    fun `should map request to command with default feeLevel`() {
        // given
        val sourceVaultId = UUID.randomUUID().toString()
        val transaction = aTransaction(sourceVaultId = sourceVaultId)
        val expectedCommand =
            SubmitTransactionCommand(
                externalTxId = "ext-map-001",
                sourceVaultId = sourceVaultId,
                destinationAddress = "0xmapped",
                currency = "ETH",
                protocol = "ETH",
                amount = BigDecimal("10.0"),
                feeLevel = null,
                treatAsGrossAmount = null,
                note = null,
            )
        every { transactionSubmissionHandler.handle(expectedCommand) } returns transaction

        // when / then
        mockMvc
            .perform(
                post("/api/v1/transactions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """{"externalTxId":"ext-map-001","sourceVaultId":"$sourceVaultId","destinationAddress":"0xmapped","currency":"ETH","protocol":"ETH","amount":10.0}""",
                    ),
            ).andExpect(status().isCreated)

        verify { transactionSubmissionHandler.handle(expectedCommand) }
    }

    @Test
    fun `should map FeeEstimateResult to EstimateFeeResponse`() {
        // given
        val vaultId = UUID.randomUUID()
        val vault = aVault(id = VaultId(vaultId), fireblocksVaultId = "fb-001")
        val supportedAsset = aSupportedAsset(fireblocksAssetId = "BTC")
        val feeResult =
            FeeEstimateResult(
                low = BigDecimal("0.00001"),
                medium = BigDecimal("0.00005"),
                high = BigDecimal("0.0001"),
            )
        every { vaultQueryService.getVault(VaultId(vaultId)) } returns vault
        every { supportedAssetRepository.findByCurrencyAndProtocol("BTC", "BTC") } returns supportedAsset
        every { fireblocksTransactionPort.estimateFee(any()) } returns feeResult

        // when / then
        mockMvc
            .perform(
                post("/api/v1/transactions/estimate-fee")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """{"sourceVaultId":"$vaultId","destinationAddress":"0xaddr","currency":"BTC","protocol":"BTC","amount":1.0}""",
                    ),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.low").value(0.00001))
            .andExpect(jsonPath("$.medium").value(0.00005))
            .andExpect(jsonPath("$.high").value(0.0001))
    }

    @Test
    fun `should cancel transaction and return 200`() {
        // given
        val transactionId = UUID.randomUUID()
        val transaction =
            aTransaction(
                id = TransactionId(transactionId),
                status = TransactionStatus.PROCESSING,
                fireblocksTransactionId = "fb-tx-cancel",
            )
        every { transactionCancellationHandler.handle(TransactionId(transactionId)) } returns transaction

        // when / then
        mockMvc
            .perform(post("/api/v1/transactions/$transactionId/cancel"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.transactionId").value(transactionId.toString()))
            .andExpect(jsonPath("$.status").value("PROCESSING"))
            .andExpect(jsonPath("$.message").value("Cancellation requested"))
    }

    @Test
    fun `should return 409 for terminal transaction cancellation`() {
        // given
        val transactionId = UUID.randomUUID()
        every { transactionCancellationHandler.handle(TransactionId(transactionId)) } throws
            TransactionAlreadyTerminalException(transactionId.toString(), "CONFIRMED")

        // when / then
        mockMvc
            .perform(post("/api/v1/transactions/$transactionId/cancel"))
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.code").value("CUSTODY-2011"))
    }

    @Test
    fun `should return 409 for not-cancellable transaction`() {
        // given
        val transactionId = UUID.randomUUID()
        every { transactionCancellationHandler.handle(TransactionId(transactionId)) } throws
            TransactionNotCancellableException(transactionId.toString())

        // when / then
        mockMvc
            .perform(post("/api/v1/transactions/$transactionId/cancel"))
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.code").value("CUSTODY-2010"))
    }
}
