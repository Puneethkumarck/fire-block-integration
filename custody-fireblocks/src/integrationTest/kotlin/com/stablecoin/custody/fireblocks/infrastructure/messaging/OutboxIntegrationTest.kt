package com.stablecoin.custody.fireblocks.infrastructure.messaging

import com.stablecoin.custody.fireblocks.AbstractIntegrationTest
import com.stablecoin.custody.fireblocks.domain.event.VaultCreatedEvent
import com.stablecoin.custody.fireblocks.domain.port.EventPublisher
import com.stablecoin.custody.fireblocks.test.fixtures.aVaultCreatedEvent
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.transaction.annotation.Transactional

class OutboxIntegrationTest : AbstractIntegrationTest() {
    @Autowired
    private lateinit var vaultEventPublisher: EventPublisher<VaultCreatedEvent>

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @Test
    @Transactional
    fun `should persist outbox record when publishing vault created event`() {
        // given
        val event = aVaultCreatedEvent()

        // when
        vaultEventPublisher.publish(event)

        // then
        val count =
            jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM custody_outbox_record WHERE record_type = ?",
                Long::class.java,
                VaultCreatedEvent::class.qualifiedName,
            )
        assertThat(count).isGreaterThan(0)
    }
}
