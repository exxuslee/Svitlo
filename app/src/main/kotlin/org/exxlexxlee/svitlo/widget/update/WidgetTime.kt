package org.exxlexxlee.svitlo.widget.update

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.Clock

private const val HALF_HOUR_MS = 30 * 60 * 1000L

internal fun nextWidgetTimeUpdate(epochMillis: Long): Long =
    (Math.floorDiv(epochMillis, HALF_HOUR_MS) + 1) * HALF_HOUR_MS

/** Keeps an active Glance session current even when Room has no new data. */
internal fun observeWidgetTime(clock: Clock) = flow {
    while (true) {
        val now = clock.now().toEpochMilliseconds()
        emit(Math.floorDiv(now, HALF_HOUR_MS))
        // Also notice manual clock changes and resume after sleep without a long stale delay.
        delay((nextWidgetTimeUpdate(now) - now).coerceIn(1, 60_000))
    }
}.distinctUntilChanged()
