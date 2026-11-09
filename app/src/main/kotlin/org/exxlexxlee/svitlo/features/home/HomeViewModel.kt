package org.exxlexxlee.svitlo.features.home

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import org.exxlexxlee.domain.model.KyivTime
import org.exxlexxlee.domain.model.Place
import org.exxlexxlee.domain.usecase.DeletePlace
import org.exxlexxlee.domain.usecase.ObservePlaceSchedules
import org.exxlexxlee.domain.usecase.RefreshSchedule
import org.exxlexxlee.svitlo.features.home.models.Action
import org.exxlexxlee.svitlo.features.home.models.Event
import org.exxlexxlee.svitlo.features.home.models.ViewState
import org.exxlexxlee.svitlo.ui.common.BaseViewModel
import org.exxlexxlee.svitlo.widget.WidgetManager
import kotlin.time.Duration.Companion.milliseconds

class HomeViewModel(
    observePlaceSchedules: ObservePlaceSchedules,
    private val refreshSchedule: RefreshSchedule,
    private val deletePlace: DeletePlace,
    private val clock: Clock,
    private val widgetManager: WidgetManager,
) : BaseViewModel<ViewState, Action, Event>(ViewState()) {

    private val refreshing = MutableStateFlow(false)
    private val errors = MutableStateFlow<Map<Long, String>>(emptyMap())
    private val minuteOfDay = flow {
        while (currentCoroutineContext().isActive) {
            emit(KyivTime.minuteOfDay(clock))
            delay(30_000.milliseconds)
        }
    }
    private var latestPlaces: List<Place> = emptyList()
    private var refreshJob: Job? = null

    init {
        viewModelScope.launch {
            val schedules = observePlaceSchedules().onEach { sources ->
                val places = sources.map { it.place }
                if (places != latestPlaces) {
                    latestPlaces = places
                    refreshAll(places)
                }
            }
            combine(schedules, refreshing, errors, minuteOfDay) { cards, isRefreshing, errorMap, minute ->
                val mappedCards = cards.map { source ->
                    PlaceCardMapper.map(
                        place = source.place,
                        schedule = source.schedule,
                        minuteOfDay = minute,
                        error = errorMap[source.place.id],
                    )
                }
                ViewState(
                    loaded = true,
                    isRefreshing = isRefreshing,
                    cards = mappedCards,
                    showWidgetBanner = shouldShowWidgetBanner(mappedCards.size),
                )
            }.collect { incoming ->
                viewState = incoming.copy(
                    pendingDeleteId = viewState.pendingDeleteId,
                    showWidgetBanner = shouldShowWidgetBanner(incoming.cards.size),
                )
            }
        }
    }

    override fun obtainEvent(viewEvent: Event) {
        when (viewEvent) {
            Event.Add -> viewAction = Action.OpenAddress()
            Event.Refresh -> refreshAll(latestPlaces)
            is Event.DeleteRequested -> viewState = viewState.copy(pendingDeleteId = viewEvent.id)
            Event.DismissDelete -> viewState = viewState.copy(pendingDeleteId = null)
            Event.ConfirmDelete -> {
                val id = viewState.pendingDeleteId ?: return
                viewState = viewState.copy(pendingDeleteId = null)
                viewModelScope.launch { deletePlace(id) }
            }
            is Event.Edit -> viewAction = Action.OpenAddress(viewEvent.id)
            Event.Settings -> viewAction = Action.OpenSettings
            Event.AddWidgetClicked -> viewAction = Action.RequestPinWidget
            Event.DismissWidgetBanner -> {
                widgetManager.setBannerDismissed(true)
                viewState = viewState.copy(showWidgetBanner = false)
            }
            Event.CheckWidget -> {
                val shouldShow = shouldShowWidgetBanner(viewState.cards.size)
                if (viewState.showWidgetBanner != shouldShow) {
                    viewState = viewState.copy(showWidgetBanner = shouldShow)
                }
            }
        }
    }

    private fun shouldShowWidgetBanner(cardsCount: Int): Boolean {
        return (cardsCount > 0) &&
            !widgetManager.hasInstalledWidgets() &&
            !widgetManager.isBannerDismissed()
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
}
