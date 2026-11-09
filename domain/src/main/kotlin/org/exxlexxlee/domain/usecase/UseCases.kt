package org.exxlexxlee.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.exxlexxlee.domain.model.City
import org.exxlexxlee.domain.model.DaySchedule
import org.exxlexxlee.domain.model.Dial
import org.exxlexxlee.domain.model.Dials
import org.exxlexxlee.domain.model.HourState
import org.exxlexxlee.domain.model.House
import org.exxlexxlee.domain.model.OutageGroup
import org.exxlexxlee.domain.model.OutageSchedule
import org.exxlexxlee.domain.model.Place
import org.exxlexxlee.domain.model.Region
import org.exxlexxlee.domain.model.Selection
import org.exxlexxlee.domain.model.ServiceArea
import org.exxlexxlee.domain.model.Street
import org.exxlexxlee.domain.repository.LocationRepository
import org.exxlexxlee.domain.repository.ScheduleRepository
import org.exxlexxlee.domain.repository.SelectionRepository
import org.exxlexxlee.domain.repository.SettingsRepository

class GetRegions(private val locationRepository: LocationRepository) {
    suspend operator fun invoke(): List<Region> = locationRepository.regions()
}

class GetAreas(private val locationRepository: LocationRepository) {
    suspend operator fun invoke(): List<ServiceArea> = locationRepository.areas()
}

class GetCities(private val locationRepository: LocationRepository) {
    suspend operator fun invoke(regionId: String): List<City> = locationRepository.cities(regionId)
}

class GetStreets(private val locationRepository: LocationRepository) {
    suspend operator fun invoke(cityId: String): List<Street> = locationRepository.streets(cityId)
}

class GetHouses(private val locationRepository: LocationRepository) {
    suspend operator fun invoke(streetId: String): List<House> = locationRepository.houses(streetId)
}

class GetGroups(private val locationRepository: LocationRepository) {
    suspend operator fun invoke(regionId: String): List<OutageGroup> =
        locationRepository.groups(regionId)
}

class SaveSelection(private val selectionRepository: SelectionRepository) {
    suspend operator fun invoke(selection: Selection, placeId: Long? = null): Long =
        selectionRepository.save(selection, placeId)
}

class ObserveSelection(private val selectionRepository: SelectionRepository) {
    operator fun invoke(): Flow<Selection?> = selectionRepository.observe()
}

class ObservePlaces(private val selectionRepository: SelectionRepository) {
    operator fun invoke(): Flow<List<Place>> = selectionRepository.observePlaces()
}

class ObservePlace(private val selectionRepository: SelectionRepository) {
    operator fun invoke(id: Long): Flow<Place?> = selectionRepository.observePlace(id)
}

class DeletePlace(private val selectionRepository: SelectionRepository) {
    suspend operator fun invoke(id: Long) = selectionRepository.delete(id)
}

class ObserveSchedule(private val scheduleRepository: ScheduleRepository) {
    operator fun invoke(selection: Selection): Flow<OutageSchedule?> =
        scheduleRepository.observe(selection)
}

class RefreshSchedule(private val scheduleRepository: ScheduleRepository) {
    suspend operator fun invoke(selection: Selection): Result<Unit> =
        scheduleRepository.refresh(selection)
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

class ObserveDarkTheme(private val settingsRepository: SettingsRepository) {
    operator fun invoke(): Flow<Boolean> = settingsRepository.observeDarkTheme()
}

class GetDarkTheme(private val settingsRepository: SettingsRepository) {
    operator fun invoke(): Boolean = settingsRepository.isDarkTheme()
}

class SetDarkTheme(private val settingsRepository: SettingsRepository) {
    suspend operator fun invoke(isDark: Boolean) = settingsRepository.setDarkTheme(isDark)
}
