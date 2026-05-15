package com.stablecoin.custody.fireblocks.domain.shared

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class StateMachineTest {
    private enum class Status {
        PENDING,
        ACTIVE,
        FAILED,
    }

    private data class TestEntity(
        val id: String,
        val status: Status,
    ) : StateProvider<Status> {
        override fun currentState(): Status = status
    }

    private class TestTransitionException(
        val entity: TestEntity,
        val target: Status,
    ) : RuntimeException("Cannot transition ${entity.id} from ${entity.status} to $target")

    private val stateMachine =
        StateMachine<Status, TestEntity>(
            transitions =
                mapOf(
                    Status.PENDING to setOf(Status.ACTIVE, Status.FAILED),
                    Status.ACTIVE to setOf(Status.FAILED),
                ),
            onInvalidTransition = { entity, target -> throw TestTransitionException(entity, target) },
        )

    @Test
    fun `should allow valid transition`() {
        // given
        val entity = TestEntity(id = "e-1", status = Status.PENDING)

        // when
        val result = stateMachine.transition(entity, Status.ACTIVE)

        // then
        assertThat(result).isEqualTo(Status.ACTIVE)
    }

    @Test
    fun `should reject invalid transition`() {
        // given
        val entity = TestEntity(id = "e-1", status = Status.FAILED)

        // when/then
        assertThatThrownBy { stateMachine.transition(entity, Status.ACTIVE) }
            .isInstanceOf(TestTransitionException::class.java)
            .hasMessageContaining("Cannot transition e-1 from FAILED to ACTIVE")
    }

    @Test
    fun `should throw custom exception on invalid transition`() {
        // given
        val entity = TestEntity(id = "e-2", status = Status.ACTIVE)

        // when/then
        assertThatThrownBy { stateMachine.transition(entity, Status.PENDING) }
            .isInstanceOf(TestTransitionException::class.java)
    }

    @Test
    fun `should pass both entity and target to onInvalidTransition callback`() {
        // given
        val entity = TestEntity(id = "e-3", status = Status.FAILED)

        // when/then
        assertThatThrownBy { stateMachine.transition(entity, Status.PENDING) }
            .isInstanceOf(TestTransitionException::class.java)
            .satisfies({ ex ->
                val transitionEx = ex as TestTransitionException
                assertThat(transitionEx.entity).isEqualTo(entity)
                assertThat(transitionEx.target).isEqualTo(Status.PENDING)
            })
    }
}
