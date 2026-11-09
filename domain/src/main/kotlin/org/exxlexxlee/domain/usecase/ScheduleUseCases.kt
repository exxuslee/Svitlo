package org.exxlexxlee.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.exxlexxlee.domain.model.DaySchedule
import org.exxlexxlee.domain.model.Dial
import org.exxlexxlee.domain.model.Dials
import org.exxlexxlee.domain.model.HourState
import org.exxlexxlee.domain.model.OutageSchedule
import org.exxlexxlee.domain.model.Selection
import org.exxlexxlee.domain.repository.ScheduleRepository

class ObserveSchedule(private val scheduleRepository: ScheduleRepository) {
    operator fun invoke(selection: Selection): Flow<OutageSchedule?> =
        scheduleRepository.observe(selection)
}

class RefreshSchedule(private val scheduleRepository: ScheduleRepository) {
    suspend operator fun invoke(selection: Selection): Result<Unit> =
        scheduleRepository.refresh(selection)
}

class RefreshSchedules(private val scheduleRepository: ScheduleRepository) {
    suspend operator fun invoke(selections: List<Selection>): Map<Selection, Result<Unit>> =
        scheduleRepository.refreshAll(selections)
}

class ObserveDials(private val observeSchedule: ObserveSchedule) {
    operator fun invoke(selection: Selection): Flow<Dials?> =
        observeSchedule(selection).map { schedule -> schedule?.let { toDials(it) } }

    companion object {
        fun toDials(schedule: OutageSchedule): Dials {
            val (todayAm, todayPm) = splitDay(schedule.today)
            val tomorrow = schedule.tomorrow?.let { splitDay(it) }
            return Dials(
                todayAm = todayAm,
                todayPm = todayPm,
                tomorrowAm = tomorrow?.first,
                tomorrowPm = tomorrow?.second,
            )
        }

        fun splitDay(day: DaySchedule): Pair<Dial, Dial> {
            val am = Dial((0 until 12).map { hour -> hourState(day, hour) })
            val pm = Dial((12 until 24).map { hour -> hourState(day, hour) })
            return am to pm
        }

        private fun hourState(day: DaySchedule, hour: Int): HourState {
            val index = hour * 2
            return HourState(
                first = day.halfHours[index],
                second = day.halfHours[index + 1],
            )
        }
    }
}
