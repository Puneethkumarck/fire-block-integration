package com.stablecoin.custody.fireblocks

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

class FlywayMigrationIntegrationTest : AbstractIntegrationTest() {
    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @Test
    fun `should apply all flyway migrations successfully`() {
        // given
        val tables =
            jdbcTemplate.queryForList(
                """
            SELECT table_name FROM information_schema.tables
            WHERE table_schema = 'public'
            ORDER BY table_name
            """,
                String::class.java,
            )

        // then
        assertThat(tables).containsExactlyInAnyOrder(
            "audit_logs",
            "custody_outbox_instance",
            "custody_outbox_partition",
            "custody_outbox_record",
            "deposit_addresses",
            "flyway_schema_history",
            "fund_allocations",
            "internal_balances",
            "reconciliation_results",
            "shedlock",
            "supported_assets",
            "transactions",
            "vaults",
            "wallet_assets",
        )
    }

    @Test
    fun `should insert vault and enforce customer_ref_id uniqueness`() {
        // given
        val vaultId = UUID.randomUUID()
        val now = OffsetDateTime.now()
        jdbcTemplate.update(
            """
            INSERT INTO vaults (id, customer_ref_id, name, status, created_at, updated_at, version)
            VALUES (?, ?, ?, ?, ?, ?, 0)
            """,
            vaultId,
            "unique-customer-001",
            "Test Vault",
            "PENDING",
            now,
            now,
        )

        // when/then
        assertThatThrownBy {
            jdbcTemplate.update(
                """
                INSERT INTO vaults (id, customer_ref_id, name, status, created_at, updated_at, version)
                VALUES (?, ?, ?, ?, ?, ?, 0)
                """,
                UUID.randomUUID(),
                "unique-customer-001",
                "Duplicate",
                "PENDING",
                now,
                now,
            )
        }.hasMessageContaining("customer_ref_id")
    }

    @Test
    fun `should enforce wallet_assets composite unique constraint`() {
        // given
        val vaultId = UUID.randomUUID()
        val now = OffsetDateTime.now()
        jdbcTemplate.update(
            """
            INSERT INTO vaults (id, customer_ref_id, name, status, created_at, updated_at, version)
            VALUES (?, ?, ?, ?, ?, ?, 0)
            """,
            vaultId,
            "wallet-unique-test-${UUID.randomUUID()}",
            "Vault",
            "ACTIVE",
            now,
            now,
        )

        jdbcTemplate.update(
            """
            INSERT INTO wallet_assets (id, vault_id, currency, protocol, fireblocks_asset_id, status, created_at, updated_at, version)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, 0)
            """,
            UUID.randomUUID(),
            vaultId,
            "ETH",
            "ETH",
            "ETH",
            "ACTIVE",
            now,
            now,
        )

        // when/then
        assertThatThrownBy {
            jdbcTemplate.update(
                """
                INSERT INTO wallet_assets (id, vault_id, currency, protocol, fireblocks_asset_id, status, created_at, updated_at, version)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, 0)
                """,
                UUID.randomUUID(),
                vaultId,
                "ETH",
                "ETH",
                "ETH_DUP",
                "ACTIVE",
                now,
                now,
            )
        }.hasMessageContaining("wallet_assets_vault_id_currency_protocol_key")
    }

    @Test
    fun `should enforce transactions external_tx_id uniqueness`() {
        // given
        val externalTxId = "tx-unique-${UUID.randomUUID()}"
        val now = OffsetDateTime.now()
        jdbcTemplate.update(
            """
            INSERT INTO transactions (id, external_tx_id, status, asset_id, currency, protocol, amount,
                source_vault_id, destination_address, created_at, updated_at, version)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0)
            """,
            UUID.randomUUID(),
            externalTxId,
            "CREATED",
            "ETH",
            "ETH",
            "ETH",
            BigDecimal("1.500000000000000000"),
            "vault-1",
            "0xabc123",
            now,
            now,
        )

        // when/then
        assertThatThrownBy {
            jdbcTemplate.update(
                """
                INSERT INTO transactions (id, external_tx_id, status, asset_id, currency, protocol, amount,
                    source_vault_id, destination_address, created_at, updated_at, version)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0)
                """,
                UUID.randomUUID(),
                externalTxId,
                "CREATED",
                "ETH",
                "ETH",
                "ETH",
                BigDecimal("2.000000000000000000"),
                "vault-2",
                "0xdef456",
                now,
                now,
            )
        }.hasMessageContaining("external_tx_id")
    }

    @Test
    fun `should store transaction amount with NUMERIC 36 18 precision`() {
        // given
        val txId = UUID.randomUUID()
        val preciseAmount = BigDecimal("123456789012345678.123456789012345678")
        val now = OffsetDateTime.now()
        jdbcTemplate.update(
            """
            INSERT INTO transactions (id, external_tx_id, status, asset_id, currency, protocol, amount,
                source_vault_id, destination_address, created_at, updated_at, version)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0)
            """,
            txId,
            "precision-test-${UUID.randomUUID()}",
            "CREATED",
            "BTC",
            "BTC",
            "BTC",
            preciseAmount,
            "vault-1",
            "bc1qtest",
            now,
            now,
        )

        // when
        val storedAmount =
            jdbcTemplate.queryForObject(
                "SELECT amount FROM transactions WHERE id = ?",
                BigDecimal::class.java,
                txId,
            )

        // then
        assertThat(storedAmount).isEqualByComparingTo(preciseAmount)
    }

    @Test
    fun `should reject UPDATE on audit_logs via immutability trigger`() {
        // given
        val auditId = UUID.randomUUID()
        val now = OffsetDateTime.now()
        jdbcTemplate.update(
            """
            INSERT INTO audit_logs (id, timestamp, actor, operation, resource_id, status)
            VALUES (?, ?, ?, ?, ?, ?)
            """,
            auditId,
            now,
            "system",
            "VAULT_CREATED",
            "vault-123",
            "SUCCESS",
        )

        // when/then
        assertThatThrownBy {
            jdbcTemplate.update(
                "UPDATE audit_logs SET status = 'FAILURE' WHERE id = ?",
                auditId,
            )
        }.hasMessageContaining("audit_logs table is immutable")
    }

    @Test
    fun `should reject DELETE on audit_logs via immutability trigger`() {
        // given
        val auditId = UUID.randomUUID()
        val now = OffsetDateTime.now()
        jdbcTemplate.update(
            """
            INSERT INTO audit_logs (id, timestamp, actor, operation, resource_id, status)
            VALUES (?, ?, ?, ?, ?, ?)
            """,
            auditId,
            now,
            "system",
            "TRANSACTION_SUBMITTED",
            "tx-456",
            "SUCCESS",
        )

        // when/then
        assertThatThrownBy {
            jdbcTemplate.update(
                "DELETE FROM audit_logs WHERE id = ?",
                auditId,
            )
        }.hasMessageContaining("audit_logs table is immutable")
    }

    @Test
    fun `should have supported_assets seed data`() {
        // when
        val assets =
            jdbcTemplate.queryForList(
                "SELECT currency, protocol, fireblocks_asset_id, taggable, utxo FROM supported_assets ORDER BY currency",
            )

        // then
        assertThat(assets).hasSize(4)
        assertThat(assets.map { it["currency"] }).containsExactly("BTC", "ETH", "EURC", "SOL")
        assertThat(assets.map { it["protocol"] }).containsExactly("BTC", "ETH", "ETH", "SOL")
        assertThat(assets.map { it["fireblocks_asset_id"] }).containsExactly("BTC", "ETH", "EURC", "SOL")
    }

    @Test
    fun `should enforce supported_assets currency_protocol uniqueness`() {
        // when/then
        assertThatThrownBy {
            jdbcTemplate.update(
                """
                INSERT INTO supported_assets (id, currency, protocol, fireblocks_asset_id)
                VALUES (?, ?, ?, ?)
                """,
                UUID.randomUUID(),
                "BTC",
                "BTC",
                "BTC_DUPLICATE",
            )
        }.hasMessageContaining("uq_supported_assets_currency_protocol")
    }

    @Test
    fun `should enforce supported_assets fireblocks_asset_id uniqueness`() {
        // when/then
        assertThatThrownBy {
            jdbcTemplate.update(
                """
                INSERT INTO supported_assets (id, currency, protocol, fireblocks_asset_id)
                VALUES (?, ?, ?, ?)
                """,
                UUID.randomUUID(),
                "XRP",
                "XRP",
                "BTC",
            )
        }.hasMessageContaining("fireblocks_asset_id")
    }

    @Test
    fun `should enforce foreign key from wallet_assets to vaults`() {
        // when/then
        assertThatThrownBy {
            jdbcTemplate.update(
                """
                INSERT INTO wallet_assets (id, vault_id, currency, protocol, fireblocks_asset_id, status, created_at, updated_at, version)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, 0)
                """,
                UUID.randomUUID(),
                UUID.randomUUID(),
                "ETH",
                "ETH",
                "ETH",
                "ACTIVE",
                OffsetDateTime.now(),
                OffsetDateTime.now(),
            )
        }.hasMessageContaining("vaults")
    }

    @Test
    fun `should enforce foreign key from deposit_addresses to wallet_assets`() {
        // when/then
        assertThatThrownBy {
            jdbcTemplate.update(
                """
                INSERT INTO deposit_addresses (id, wallet_asset_id, address, created_at, updated_at, version)
                VALUES (?, ?, ?, ?, ?, 0)
                """,
                UUID.randomUUID(),
                UUID.randomUUID(),
                "0xorphan",
                OffsetDateTime.now(),
                OffsetDateTime.now(),
            )
        }.hasMessageContaining("wallet_assets")
    }
}
