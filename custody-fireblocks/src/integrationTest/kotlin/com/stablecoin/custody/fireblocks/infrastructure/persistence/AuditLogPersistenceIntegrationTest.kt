package com.stablecoin.custody.fireblocks.infrastructure.persistence

import com.stablecoin.custody.fireblocks.AbstractIntegrationTest
import com.stablecoin.custody.fireblocks.domain.audit.AuditLogRepository
import com.stablecoin.custody.fireblocks.test.fixtures.anAuditLog
import jakarta.persistence.EntityManager
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Transactional
class AuditLogPersistenceIntegrationTest : AbstractIntegrationTest() {
    @Autowired
    private lateinit var auditRepository: AuditLogRepository

    @Autowired
    private lateinit var entityManager: EntityManager

    @Test
    fun `should save audit log and retrieve by resource ID`() {
        // given
        val resourceId = "vault-${UUID.randomUUID()}"
        val auditLog = anAuditLog(resourceId = resourceId)
        val saved = auditRepository.save(auditLog)
        entityManager.clear()

        // when
        val result = auditRepository.findByResourceId(resourceId)

        // then
        assertThat(result).hasSize(1)
        assertThat(result[0])
            .usingRecursiveComparison()
            .ignoringFields("timestamp")
            .isEqualTo(saved)
    }

    @Test
    fun `should persist JSONB details correctly`() {
        // given
        val details = mapOf("vaultName" to "Treasury", "amount" to 1000, "tags" to listOf("critical", "minting"))
        val auditLog = anAuditLog(details = details)
        val saved = auditRepository.save(auditLog)
        entityManager.clear()

        // when
        val result = auditRepository.findByResourceId(saved.resourceId)

        // then
        assertThat(result).hasSize(1)
        assertThat(result[0])
            .usingRecursiveComparison()
            .ignoringFields("timestamp")
            .isEqualTo(saved)
    }

    @Test
    fun `should reject UPDATE on audit_logs table`() {
        // given
        val auditLog = anAuditLog()
        val saved = auditRepository.save(auditLog)
        entityManager.flush()
        entityManager.clear()

        // when/then
        assertThatThrownBy {
            entityManager
                .createNativeQuery("UPDATE audit_logs SET actor = 'hacker' WHERE id = :id")
                .setParameter("id", saved.id)
                .executeUpdate()
        }.rootCause()
            .hasMessageContaining("immutable")
    }

    @Test
    fun `should reject DELETE on audit_logs table`() {
        // given
        val auditLog = anAuditLog()
        val saved = auditRepository.save(auditLog)
        entityManager.flush()
        entityManager.clear()

        // when/then
        assertThatThrownBy {
            entityManager
                .createNativeQuery("DELETE FROM audit_logs WHERE id = :id")
                .setParameter("id", saved.id)
                .executeUpdate()
        }.rootCause()
            .hasMessageContaining("immutable")
    }

    @Test
    fun `should persist multiple audit logs for same resource ID`() {
        // given
        val resourceId = "vault-${UUID.randomUUID()}"
        val first = auditRepository.save(anAuditLog(resourceId = resourceId))
        val second = auditRepository.save(anAuditLog(resourceId = resourceId))
        entityManager.clear()

        // when
        val result = auditRepository.findByResourceId(resourceId)

        // then
        assertThat(result).hasSize(2)
        assertThat(result.map { it.id }).containsExactlyInAnyOrder(first.id, second.id)
    }
}
