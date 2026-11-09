package org.exxlexxlee.domain.usecase

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import org.exxlexxlee.domain.model.PlaceSchedule

class ObservePlaceSchedules(
    private val observePlaces: ObservePlaces,
    private val observeSchedule: ObserveSchedule,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(): Flow<List<PlaceSchedule>> = observePlaces().flatMapLatest { places ->
        if (places.isEmpty()) {
            flowOf(emptyList())
        } else {
            combine(places.map { place ->
                observeSchedule(place.selection).map { schedule -> PlaceSchedule(place, schedule) }
            }) { schedules -> schedules.toList() }
        }
    }
}
