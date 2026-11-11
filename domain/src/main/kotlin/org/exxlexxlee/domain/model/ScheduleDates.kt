package org.exxlexxlee.domain.model

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlin.time.Duration.Companion.milliseconds

/** Calendar dates, rather than the provider's today/tomorrow field names, own each graph. */
fun OutageSchedule.forDate(date: LocalDate): OutageSchedule {
    val days = listOfNotNull(today, tomorrow)
    return copy(
        today = days.firstOrNull { it.date == date }
            ?: DaySchedule(date, List(48) { PowerState.UNKNOWN }, published = false),
        tomorrow = days.firstOrNull { it.date == date.plus(1, DateTimeUnit.DAY) },
    )
}

fun nextKyivMidnight(clock: Clock) =
    KyivTime.today(clock).plus(1, DateTimeUnit.DAY).atStartOfDayIn(KyivTime.ZONE)

fun observeKyivDate(clock: Clock) = flow {
    while (true) {
        emit(KyivTime.today(clock))
        // Wake at midnight; also detect wall-clock changes while the process is alive.
        delay((nextKyivMidnight(clock) - clock.now()).inWholeMilliseconds.coerceIn(1, 60_000).milliseconds)
    }
}.distinctUntilChanged()
