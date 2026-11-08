package org.exxlexxlee.svitlo.features.home

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.exxlexxlee.domain.model.KyivTime
import org.exxlexxlee.domain.model.OutageSchedule
import org.exxlexxlee.domain.model.Place
import org.exxlexxlee.domain.usecase.DeletePlace
import org.exxlexxlee.domain.usecase.ObservePlaces
import org.exxlexxlee.domain.usecase.ObserveSchedule
import org.exxlexxlee.domain.usecase.RefreshSchedule
import org.exxlexxlee.svitlo.features.home.models.Action
import org.exxlexxlee.svitlo.features.home.models.Event
import org.exxlexxlee.svitlo.features.home.models.ViewState
import org.exxlexxlee.svitlo.ui.common.BaseViewModel
import kotlinx.datetime.Clock

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(
    observePlaces: ObservePlaces,
    private val observeSchedule: ObserveSchedule,
    private val refreshSchedule: RefreshSchedule,
    private val deletePlace: DeletePlace,
    private val clock: Clock,
) : BaseViewModel<ViewState, Action, Event>(ViewState()) {

    private val refreshing = MutableStateFlow(false)
    private val errors = MutableStateFlow<Map<Long, String>>(emptyMap())
    private val minuteOfDay = flow {
        while (currentCoroutineContext().isActive) {
            emit(KyivTime.minuteOfDay(clock))
            delay(30_000)
        }
    }
    private var latestPlaces: List<Place> = emptyList()
    private var refreshJob: Job? = null

    init {
        viewModelScope.launch {
            observePlaces()
                .flatMapLatest { places ->
                    val schedules = if (places.isEmpty()) {
                        flowOf(emptyList())
                    } else {
                        combine(places.map { place ->
                            observeSchedule(place.selection).map { schedule ->
                                CardSource(place, schedule)
                            }
                        }) { sources -> sources.toList() }
                    }
                    combine(schedules, refreshing, errors, minuteOfDay) { cards, isRefreshing, errorMap, minute ->
                        ViewState(
                            loaded = true,
                            isRefreshing = isRefreshing,
                            cards = cards.map { source ->
                                PlaceCardMapper.map(
                                    place = source.place,
                                    schedule = source.schedule,
                                    minuteOfDay = minute,
                                    error = errorMap[source.place.id],
                                )
                            },
                        )
                    }
                }
                .collect { incoming ->
                    viewState = incoming.copy(pendingDeleteId = viewState.pendingDeleteId)
                }
        }
        viewModelScope.launch {
            observePlaces().collect { places ->
                latestPlaces = places
                refreshAll(places)
            }
        }
    }

    override fun obtainEvent(viewEvent: Event) {
        when (viewEvent) {
            Event.Add -> viewAction = Action.OpenAddress()
            is Event.Open -> viewAction = Action.OpenSchedule(viewEvent.id)
            Event.Refresh -> refreshAll(latestPlaces)
            is Event.DeleteRequested -> viewState = viewState.copy(pendingDeleteId = viewEvent.id)
            Event.DismissDelete -> viewState = viewState.copy(pendingDeleteId = null)
            Event.ConfirmDelete -> {
                val id = viewState.pendingDeleteId ?: return
                viewState = viewState.copy(pendingDeleteId = null)
                viewModelScope.launch { deletePlace(id) }
            }
            is Event.Edit -> viewAction = Action.OpenAddress(viewEvent.id)
        }
    }

    private fun refreshAll(places: List<Place>) {
        refreshJob?.cancel()
        if (places.isEmpty()) {
            refreshing.value = false
            errors.value = emptyMap()
            return
        }
        refreshJob = viewModelScope.launch {
            refreshing.value = true
            val failures = mutableMapOf<Long, String>()
            try {
                places.forEach { place ->
                    val message = refreshSchedule(place.selection).exceptionOrNull()?.message
                    if (message != null) failures[place.id] = message
                }
                errors.value = failures
            } finally {
                if (isActive) refreshing.value = false
            }
        }
    }

    private data class CardSource(
        val place: Place,
        val schedule: OutageSchedule?,
    )
}
