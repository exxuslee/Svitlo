package org.exxlexxlee.domain.mapper

import org.exxlexxlee.domain.model.DaySchedule
import org.exxlexxlee.domain.model.Dial
import org.exxlexxlee.domain.model.Dials
import org.exxlexxlee.domain.model.HALF_HOURS_PER_HOUR
import org.exxlexxlee.domain.model.HOURS_PER_DIAL
import org.exxlexxlee.domain.model.HourState
import org.exxlexxlee.domain.model.OutageSchedule

object ScheduleDialMapper {
    fun toDials(schedule: OutageSchedule): Dials {
        val (todayAm, todayPm) = splitDay(schedule.today)
        val tomorrow = schedule.tomorrow?.let(::splitDay)
        return Dials(
            todayAm = todayAm,
            todayPm = todayPm,
            tomorrowAm = tomorrow?.first,
            tomorrowPm = tomorrow?.second,
        )
    }

    fun splitDay(day: DaySchedule): Pair<Dial, Dial> {
        val hours = day.halfHours.chunked(HALF_HOURS_PER_HOUR) { halves ->
            HourState(first = halves[0], second = halves[1])
        }
        return Dial(hours.take(HOURS_PER_DIAL)) to Dial(hours.drop(HOURS_PER_DIAL))
    }
}
