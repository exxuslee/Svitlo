package org.exxlexxlee.svitlo.widget

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.exxlexxlee.svitlo.widget.update.nextWidgetTimeUpdate
import org.exxlexxlee.svitlo.widget.update.observeWidgetTime
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WidgetTimeUpdatesTest {
    @Test
    fun alarmsAlignWithWallClockIncludingMidnightAndDst() {
        val cases = listOf(
            "2026-11-17T06:59:59.999Z" to "2026-11-17T07:00:00Z",
            "2026-11-17T07:05:00Z" to "2026-11-17T07:30:00Z",
            "2026-11-17T07:30:00Z" to "2026-11-17T08:00:00Z",
            "2026-11-17T21:59:59Z" to "2026-11-17T22:00:00Z",
            "2026-10-25T00:59:59Z" to "2026-10-25T01:00:00Z",
            "2026-03-29T00:59:59Z" to "2026-03-29T01:00:00Z",
        )
        for ((now, next) in cases) {
            assertEquals(
                now,
                Instant.parse(next).toEpochMilliseconds(),
                nextWidgetTimeUpdate(Instant.parse(now).toEpochMilliseconds()),
            )
        }
    }

    @Test
    fun activeSessionDetectsClockChangesAndResumeAfterSleep() = runTest {
        var now = Instant.parse("2026-11-17T06:05:00Z")
        val clock = object : Clock {
            override fun now() = now
        }
        val received = mutableListOf<Long>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            observeWidgetTime(clock).collect { received += it }
        }
        now = Instant.parse("2026-11-17T09:05:00Z")
        advanceTimeBy(60_000)
        runCurrent()
        assertEquals(2, received.size)
        now = Instant.parse("2026-11-17T05:05:00Z")
        advanceTimeBy(60_000)
        runCurrent()
        assertEquals(3, received.size)
        assertEquals(listOf(6L, -8L), received.zipWithNext { a, b -> b - a })
    }
}
