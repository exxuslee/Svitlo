package org.exxlexxlee.svitlo.features.home

import androidx.lifecycle.viewModelScope
import java.time.Duration
import java.time.Instant
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import org.exxlexxlee.domain.model.Country
import org.exxlexxlee.domain.model.KyivTime
import org.exxlexxlee.domain.model.OutageAddress
import org.exxlexxlee.domain.model.OutageCardState
import org.exxlexxlee.domain.model.Place
import org.exxlexxlee.domain.model.SourceAccess
import org.exxlexxlee.domain.repository.InternationalOutageRepository
import org.exxlexxlee.domain.usecase.DeletePlace
import org.exxlexxlee.domain.usecase.ObservePlaceSchedules
import org.exxlexxlee.domain.usecase.RefreshSchedules
import org.exxlexxlee.svitlo.features.home.models.Action
import org.exxlexxlee.svitlo.features.home.models.Event
import org.exxlexxlee.svitlo.features.home.models.ViewState
import org.exxlexxlee.svitlo.features.international.InternationalCardMapper
import org.exxlexxlee.svitlo.common.BaseViewModel
import org.exxlexxlee.svitlo.widget.WidgetManager

class HomeViewModel(
    observePlaceSchedules: ObservePlaceSchedules,
    private val refreshSchedules: RefreshSchedules,
    private val deletePlace: DeletePlace,
    private val clock: Clock,
    private val widgetManager: WidgetManager,
    private val internationalRepository: InternationalOutageRepository,
) : BaseViewModel<ViewState, Action, Event>(ViewState()) {

    private val schedulesRefreshing = MutableStateFlow(false)
    private val internationalRefreshing = MutableStateFlow(false)
    private val refreshing = combine(schedulesRefreshing, internationalRefreshing) { schedules, international ->
        schedules || international
    }
    private val internationalCards = MutableStateFlow<Map<Long, PlaceCardUi>>(emptyMap())
    private var internationalPlaces: List<Place> = emptyList()
    private var internationalRefreshJob: Job? = null
    private val errors = MutableStateFlow<Map<Long, String>>(emptyMap())
    private val timeTick = flow {
        while (currentCoroutineContext().isActive) {
            emit(Unit)
            delay(30_000.milliseconds)
        }
    }
    private var latestPlaces: List<Place> = emptyList()
    private var refreshJob: Job? = null

    init {
        viewModelScope.launch {
            // Incident feeds change more often than day schedules.
            while (isActive) {
                delay(300_000)
                refreshInternational()
            }
        }
        viewModelScope.launch {
            val schedules = observePlaceSchedules().onEach { sources ->
                val places = sources.map { it.place }
                if (places != latestPlaces) {
                    latestPlaces = places
                    updateInternationalPlaces(places)
                    refreshAll(places)
                }
            }
            combine(
                schedules,
                refreshing,
                errors,
                timeTick,
                internationalCards,
            ) { cards, isRefreshing, errorMap, _, savedCards ->
                val now = now()
                val mappedCards = cards.map { source ->
                    savedCards[source.place.id]?.copy(now = now) ?: PlaceCardMapper.map(
                        place = source.place,
                        schedule = source.schedule,
                        minuteOfDay = KyivTime.minuteOfDay(clock),
                        error = errorMap[source.place.id],
                        now = now,
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
            Event.Add -> sendAction(Action.OpenAddress())
            Event.Refresh -> {
                refreshAll(latestPlaces)
                refreshInternational()
            }
            is Event.DeleteRequested -> viewState = viewState.copy(pendingDeleteId = viewEvent.id)
            Event.DismissDelete -> viewState = viewState.copy(pendingDeleteId = null)
            Event.ConfirmDelete -> {
                val id = viewState.pendingDeleteId ?: return
                viewState = viewState.copy(pendingDeleteId = null)
                viewModelScope.launch { deletePlace(id) }
            }

            is Event.Edit -> sendAction(Action.OpenAddress(viewEvent.id))
            Event.Settings -> sendAction(Action.OpenSettings)
            Event.AddWidgetClicked -> sendAction(Action.RequestPinWidget)
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
            schedulesRefreshing.value = false
            errors.value = emptyMap()
            return
        }
        refreshJob = viewModelScope.launch {
            schedulesRefreshing.value = true
            val failures = mutableMapOf<Long, String>()
            try {
                val results = refreshSchedules(places.map { it.selection })
                places.forEach { place ->
                    val message = results[place.selection]?.exceptionOrNull()?.message
                    if (message != null) failures[place.id] = message
                }
                errors.value = failures
            } finally {
                if (isActive) schedulesRefreshing.value = false
            }
        }
    }

    private fun now(): Instant = Instant.parse(clock.now().toString())

    private fun updateInternationalPlaces(places: List<Place>) {
        val incoming = places.filter {
            it.selection.details?.country?.let { country -> country != Country.UKRAINE } == true
        }
        if (incoming == internationalPlaces) return
        val unchanged = incoming.filter { it in internationalPlaces }.map { it.id }.toSet()
        internationalCards.value = internationalCards.value.filterKeys { it in unchanged }
        internationalPlaces = incoming
        refreshInternational()
    }

    private fun refreshInternational() {
        internationalRefreshJob?.cancel()
        val current = internationalPlaces
        internationalCards.value = current.mapNotNull { place ->
            (internationalCards.value[place.id] ?: internationalPlaceholder(place))?.let { place.id to it }
        }.toMap()
        if (current.isEmpty()) {
            internationalRefreshing.value = false
            return
        }
        internationalRefreshJob = viewModelScope.launch {
            internationalRefreshing.value = true
            try {
                for (place in current) {
                    refreshInternationalPlace(place)
                }
            } finally {
                if (isActive) internationalRefreshing.value = false
            }
        }
    }

    private suspend fun refreshInternationalPlace(place: Place) {
        val details = place.selection.details ?: return
        val source = internationalRepository.sources(details.country).find { it.id == details.sourceId }
            ?: return
        if (source.access !in listOf(SourceAccess.LIVE, SourceAccess.ADDRESS)) {
            internationalCards.value[place.id]?.let { card ->
                internationalCards.value = internationalCards.value + (place.id to card.copy(outageState = OutageCardState.Error))
            }
            return
        }
        try {
            val address = details.cityId?.let { city ->
                details.streetId?.let { street ->
                    OutageAddress(
                        city,
                        street,
                        details.house
                    )
                }
            }
            val events = internationalRepository.outages(source, address).filter {
                (it.location + " " + it.description).contains(
                    details.filter,
                    true
                )
            }
            currentCoroutineContext().ensureActive()
            val now = now()
            internationalCards.value = internationalCards.value + (place.id to InternationalCardMapper.map(
                events,
                source,
                now,
                details.filter.ifBlank { details.name },
                details.locationPoint,
            ).copy(id = place.id, title = details.name, iconId = details.iconId))
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            val card = internationalCards.value[place.id] ?: return
            val cached = when (val state = card.outageState) {
                is OutageCardState.Content -> state.snapshot
                is OutageCardState.Stale -> state.snapshot
                else -> null
            }
            val now = now()
            internationalCards.value = internationalCards.value + (place.id to card.copy(
                now = now,
                outageState = cached?.let {
                    OutageCardState.Stale(
                        it,
                        Duration.between(it.fetchedAt, now).coerceAtLeast(Duration.ZERO),
                    )
                } ?: OutageCardState.Error,
            ))
        }
    }

    private fun internationalPlaceholder(place: Place): PlaceCardUi? {
        val details = place.selection.details ?: return null
        val source =
            internationalRepository.sources(details.country).find { it.id == details.sourceId } ?: return null
        return InternationalCardMapper.map(
            emptyList(), source, now(), details.name,
        ).copy(id = place.id, title = details.name, iconId = details.iconId, outageState = OutageCardState.Loading)
    }
}
