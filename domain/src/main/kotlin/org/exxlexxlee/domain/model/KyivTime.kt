package org.exxlexxlee.domain.model

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

object KyivTime {
    val ZONE: TimeZone = TimeZone.of("Europe/Kyiv")

    fun date(instant: Instant): LocalDate = instant.toLocalDateTime(ZONE).date

    fun today(clock: Clock = Clock.System): LocalDate = date(clock.now())

    fun hourOfDay(clock: Clock = Clock.System): Int = clock.now().toLocalDateTime(ZONE).hour
}
