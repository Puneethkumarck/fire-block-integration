package com.stablecoin.custody.fireblocks.infrastructure.temporal

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class TemporalConstantsTest {
    @Test
    fun `should define task queue name`() {
        assertThat(TemporalConstants.TASK_QUEUE).isEqualTo("CustodyTransactionTaskQueue")
    }

    @Test
    fun `should define workflow ID prefix`() {
        assertThat(TemporalConstants.WORKFLOW_ID_PREFIX).isEqualTo("TransactionLifecycle_")
    }
}
