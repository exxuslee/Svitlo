package org.exxlexxlee.domain.model

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

object KyivTime {
    val ZONE: TimeZone = runCatching { TimeZone.of("Europe/Kyiv") }
        .getOrElse {
            runCatching { TimeZone.of("Europe/Kiev") }
                .getOrElse { TimeZone.of("UTC+02:00") }
        }

    fun date(instant: Instant): LocalDate = instant.toLocalDateTime(ZONE).date

    fun today(clock: Clock = Clock.System): LocalDate = date(clock.now())

    fun hourOfDay(clock: Clock = Clock.System): Int = clock.now().toLocalDateTime(ZONE).hour

    fun minuteOfDay(clock: Clock = Clock.System): Int {
        val local = clock.now().toLocalDateTime(ZONE)
        return local.hour * 60 + local.minute
    }

    fun halfHourIndex(clock: Clock = Clock.System): Int {
        val local = clock.now().toLocalDateTime(ZONE)
        return local.hour * 2 + if (local.minute >= 30) 1 else 0
    }
}
