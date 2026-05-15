package com.stablecoin.custody.fireblocks.infrastructure.fireblocks.adapter

import com.stablecoin.custody.fireblocks.domain.port.FeeEstimateResult
import com.stablecoin.custody.fireblocks.domain.port.FireblocksEstimateFeeCommand
import com.stablecoin.custody.fireblocks.domain.port.FireblocksSubmitCommand
import com.stablecoin.custody.fireblocks.domain.port.TransactionResult
import com.stablecoin.custody.fireblocks.domain.transaction.FeeLevel
import com.stablecoin.custody.fireblocks.infrastructure.fireblocks.client.FireblocksTransactionClient
import com.stablecoin.custody.fireblocks.infrastructure.fireblocks.client.dto.CreateTransactionRequest
import com.stablecoin.custody.fireblocks.infrastructure.fireblocks.client.dto.DestinationTransferPeerPath
import com.stablecoin.custody.fireblocks.infrastructure.fireblocks.client.dto.FireblocksEstimateFeeRequest
import com.stablecoin.custody.fireblocks.infrastructure.fireblocks.client.dto.FireblocksEstimateFeeResponse
import com.stablecoin.custody.fireblocks.infrastructure.fireblocks.client.dto.FireblocksFeeLevel
import com.stablecoin.custody.fireblocks.infrastructure.fireblocks.client.dto.FireblocksTransactionResponse
import com.stablecoin.custody.fireblocks.infrastructure.fireblocks.client.dto.OneTimeAddress
import com.stablecoin.custody.fireblocks.infrastructure.fireblocks.client.dto.TransferPeerPath
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.web.client.HttpClientErrorException
import java.math.BigDecimal

class FireblocksTransactionAdapterTest {
    private val transactionClient = mockk<FireblocksTransactionClient>()
    private val adapter = FireblocksTransactionAdapter(transactionClient)

    @Test
    fun `should submit transaction with correct mapping`() {
        // given
        val command =
            FireblocksSubmitCommand(
                externalTxId = "ext-tx-001",
                sourceVaultId = "vault-1",
                destinationAddress = "0xdest",
                assetId = "ETH_TEST",
                amount = BigDecimal("1.5"),
                feeLevel = FeeLevel.MEDIUM,
                treatAsGrossAmount = true,
                note = "Test payment",
            )
        val expectedRequest =
            CreateTransactionRequest(
                externalTxId = "ext-tx-001",
                source = TransferPeerPath(type = "VAULT_ACCOUNT", id = "vault-1"),
                destination =
                    DestinationTransferPeerPath(
                        type = "ONE_TIME_ADDRESS",
                        oneTimeAddress = OneTimeAddress(address = "0xdest"),
                    ),
                assetId = "ETH_TEST",
                amount = "1.5",
                feeLevel = "MEDIUM",
                treatAsGrossAmount = true,
                note = "Test payment",
            )
        val response = FireblocksTransactionResponse(id = "fb-tx-001", status = "SUBMITTED", subStatus = null, txHash = null)
        every { transactionClient.createTransaction(expectedRequest) } returns response

        // when
        val result = adapter.submitTransaction(command)

        // then
        val expected = TransactionResult(id = "fb-tx-001", status = "SUBMITTED", subStatus = null, txHash = null)
        assertThat(result).usingRecursiveComparison().isEqualTo(expected)
        verify { transactionClient.createTransaction(expectedRequest) }
    }

    @Test
    fun `should get transaction by Fireblocks ID`() {
        // given
        val response =
            FireblocksTransactionResponse(
                id = "fb-tx-002",
                status = "COMPLETED",
                subStatus = null,
                txHash = "0xhash123",
            )
        every { transactionClient.getTransaction("fb-tx-002") } returns response

        // when
        val result = adapter.getTransaction("fb-tx-002")

        // then
        val expected = TransactionResult(id = "fb-tx-002", status = "COMPLETED", subStatus = null, txHash = "0xhash123")
        assertThat(result).usingRecursiveComparison().isEqualTo(expected)
    }

    @Test
    fun `should get transaction by external ID`() {
        // given
        val response =
            FireblocksTransactionResponse(
                id = "fb-tx-003",
                status = "BROADCASTING",
                subStatus = "PENDING_BLOCKCHAIN_CONFIRMATIONS",
                txHash = null,
            )
        every { transactionClient.getByExternalId("ext-tx-003") } returns response

        // when
        val result = adapter.getByExternalId("ext-tx-003")

        // then
        val expected =
            TransactionResult(
                id = "fb-tx-003",
                status = "BROADCASTING",
                subStatus = "PENDING_BLOCKCHAIN_CONFIRMATIONS",
                txHash = null,
            )
        assertThat(result).usingRecursiveComparison().isEqualTo(expected)
    }

    @Test
    fun `should return null when external ID not found`() {
        // given
        every { transactionClient.getByExternalId("nonexistent") } throws
            HttpClientErrorException.create(
                org.springframework.http.HttpStatus.NOT_FOUND,
                "Not Found",
                org.springframework.http.HttpHeaders.EMPTY,
                ByteArray(0),
                null,
            )

        // when
        val result = adapter.getByExternalId("nonexistent")

        // then
        assertThat(result).isNull()
    }

    @Test
    fun `should estimate fee and return all levels`() {
        // given
        val command =
            FireblocksEstimateFeeCommand(
                sourceVaultId = "vault-1",
                destinationAddress = "0xdest",
                assetId = "ETH_TEST",
                amount = BigDecimal("2.0"),
            )
        val expectedRequest =
            FireblocksEstimateFeeRequest(
                assetId = "ETH_TEST",
                source = TransferPeerPath(type = "VAULT_ACCOUNT", id = "vault-1"),
                destination =
                    DestinationTransferPeerPath(
                        type = "ONE_TIME_ADDRESS",
                        oneTimeAddress = OneTimeAddress(address = "0xdest"),
                    ),
                amount = "2.0",
            )
        val response =
            FireblocksEstimateFeeResponse(
                low = FireblocksFeeLevel(networkFee = "0.0001"),
                medium = FireblocksFeeLevel(networkFee = "0.0005"),
                high = FireblocksFeeLevel(networkFee = "0.001"),
            )
        every { transactionClient.estimateFee(expectedRequest) } returns response

        // when
        val result = adapter.estimateFee(command)

        // then
        val expected =
            FeeEstimateResult(
                low = BigDecimal("0.0001"),
                medium = BigDecimal("0.0005"),
                high = BigDecimal("0.001"),
            )
        assertThat(result).usingRecursiveComparison().isEqualTo(expected)
        verify { transactionClient.estimateFee(expectedRequest) }
    }
}
