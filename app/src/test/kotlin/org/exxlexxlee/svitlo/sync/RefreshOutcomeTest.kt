package org.exxlexxlee.svitlo.sync

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException

class RefreshOutcomeTest {
    private val success = Result.success(Unit)
    private val failure = Result.failure<Unit>(IOException("offline"))

    @Test fun noPlacesOrSuccessfulRefreshFinishesWithoutRetry() {
        assertEquals(RefreshOutcome.Success, refreshOutcome(emptyList(), 0))
        assertEquals(RefreshOutcome.Success, refreshOutcome(listOf(success), 0))
    }

    @Test fun totalAndPartialFailureRequestRetry() {
        assertEquals(RefreshOutcome.Retry, refreshOutcome(listOf(failure), 0))
        assertEquals(RefreshOutcome.Retry, refreshOutcome(listOf(success, failure), 1))
    }

    @Test fun persistentFailureStopsRetryingAfterThreeAttempts() {
        assertEquals(RefreshOutcome.Failure, refreshOutcome(listOf(failure), 2))
        assertEquals(RefreshOutcome.Failure, refreshOutcome(listOf(success, failure), 3))
    }

    @Test fun recoveredRefreshSucceedsEvenOnLastAttempt() {
        assertEquals(RefreshOutcome.Success, refreshOutcome(listOf(success), 2))
    }
}
