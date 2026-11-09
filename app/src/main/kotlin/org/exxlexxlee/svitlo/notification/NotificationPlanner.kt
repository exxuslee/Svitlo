package org.exxlexxlee.svitlo.notification

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import org.exxlexxlee.domain.model.KyivTime
import org.exxlexxlee.domain.model.OutageWindows
import org.exxlexxlee.domain.model.PlaceSchedule
import kotlin.math.abs

internal data class PlannedNotification(
    val id: Int,
    val triggerEpochMs: Long,
    val placeName: String,
    val outageTime: String,
    val reminderMinutes: Int,
)

internal object NotificationPlanner {
    fun plan(
        placeSchedules: List<PlaceSchedule>,
        clock: Clock,
    ): List<PlannedNotification> = buildList {
        val nowMs = clock.now().toEpochMilliseconds()
        for ((place, schedule) in placeSchedules) {
            if (schedule == null) continue
            val details = place.selection.details ?: continue
            if (details.reminderMinutes <= 0) continue

            val days = listOfNotNull(schedule.today, schedule.tomorrow).filter { it.published }
            for ((date, halfHours) in days) {
                for ((startMinutes) in OutageWindows.windows(halfHours)) {
                    val startEpochMs = LocalDateTime(
                        year = date.year,
                        monthNumber = date.monthNumber,
                        dayOfMonth = date.dayOfMonth,
                        hour = startMinutes / 60,
                        minute = startMinutes % 60,
                    ).toInstant(KyivTime.ZONE).toEpochMilliseconds()
                    val triggerEpochMs = startEpochMs - details.reminderMinutes * 60_000L
                    if (triggerEpochMs <= nowMs) continue

                    val triggerHour = Instant.fromEpochMilliseconds(triggerEpochMs)
                        .toLocalDateTime(KyivTime.ZONE).hour
                    if (details.quietNight && triggerHour !in 7..<23) continue

                    add(
                        PlannedNotification(
                            id = abs(place.id.hashCode() * 31 + startEpochMs.hashCode()),
                            triggerEpochMs = triggerEpochMs,
                            placeName = details.name.ifBlank { place.selection.group.name },
                            outageTime = OutageWindows.formatClock(startMinutes),
                            reminderMinutes = details.reminderMinutes,
                        ),
                    )
                }
            }
        }
    }
}
