package org.exxlexxlee.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.exxlexxlee.domain.mapper.ScheduleDialMapper
import org.exxlexxlee.domain.model.DaySchedule
import org.exxlexxlee.domain.model.Dial
import org.exxlexxlee.domain.model.Dials
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
        observeSchedule(selection).map { schedule -> schedule?.let(ScheduleDialMapper::toDials) }

    companion object {
        fun toDials(schedule: OutageSchedule): Dials = ScheduleDialMapper.toDials(schedule)

        fun splitDay(day: DaySchedule): Pair<Dial, Dial> = ScheduleDialMapper.splitDay(day)
    }
}
