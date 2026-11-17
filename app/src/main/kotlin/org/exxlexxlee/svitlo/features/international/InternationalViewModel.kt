package org.exxlexxlee.svitlo.features.international

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import org.exxlexxlee.domain.model.AddressOption
import org.exxlexxlee.domain.model.Country
import org.exxlexxlee.domain.model.OutageGroup
import org.exxlexxlee.domain.model.OutageSource
import org.exxlexxlee.domain.model.Region
import org.exxlexxlee.domain.model.Selection
import org.exxlexxlee.domain.model.SourceAccess
import org.exxlexxlee.domain.repository.InternationalOutageRepository
import org.exxlexxlee.domain.repository.SelectionRepository
import org.exxlexxlee.svitlo.features.address.models.Event as DetailsEvent
import org.exxlexxlee.svitlo.features.international.models.Action
import org.exxlexxlee.svitlo.features.international.models.Event
import org.exxlexxlee.svitlo.features.international.models.InternationalStep
import org.exxlexxlee.svitlo.features.international.models.ViewState
import org.exxlexxlee.svitlo.common.BaseViewModel

class InternationalViewModel(
    private val selections: SelectionRepository,
    private val repository: InternationalOutageRepository,
    savedStateHandle: SavedStateHandle = SavedStateHandle(),
) : BaseViewModel<ViewState, Action, Event>(ViewState(Country.UKRAINE)) {
    private var request: Job? = null
    private val editingPlaceId: Long? = savedStateHandle.get<Long>("placeId")?.takeIf { it > 0 }

    init {
        editingPlaceId?.let { id ->
            request = viewModelScope.launch {
                val selection =
                    selections.observePlace(id).firstOrNull()?.selection ?: return@launch
                val details = selection.details ?: return@launch
                if (details.country == Country.UKRAINE) return@launch
                val sources = repository.sources(details.country)
                val source = sources.find { it.id == details.sourceId } ?: return@launch
                viewState = ViewState(
                    country = details.country, sources = sources, source = source,
                    step = InternationalStep.Details, query = details.filter,
                    city = details.cityId?.let { AddressOption(it, "") },
                    street = details.streetId?.let { AddressOption(it, "") },
                    house = details.house, details = details, nameEdited = true, isEditing = true,
                )
            }
        }
    }

    override fun obtainEvent(viewEvent: Event) {
        when (viewEvent) {
            is Event.CountrySelected -> selectCountry(viewEvent.country)
            is Event.SourceSelected -> select(viewEvent.source)
            is Event.QueryChanged -> query(viewEvent.value)
            is Event.LocationSelected -> {
                query(viewEvent.location)
                next()
            }
            is Event.CityQueryChanged -> cityQuery(viewEvent.value)
            is Event.StreetQueryChanged -> streetQuery(viewEvent.value)
            is Event.HouseChanged -> house(viewEvent.value)
            is Event.OptionSelected -> choose(viewEvent.option)
            is Event.DetailsChanged -> {
                if (viewEvent.event == DetailsEvent.Save) save() else detailsEvent(viewEvent.event)
            }
            Event.Search -> search(viewState.step == InternationalStep.Street)
            Event.Refresh -> refresh()
            Event.Next -> next()
            Event.Back -> if (!back()) sendAction(Action.OpenCountry)
            Event.Close -> if (!viewState.saving) sendAction(Action.Close)
            Event.Save -> save()
        }
    }

    private fun selectCountry(country: Country) {
        request?.cancel()
        viewState = ViewState(country = country, sources = repository.sources(country))
    }

    private fun select(source: OutageSource) {
        if (viewState.saving || source !in viewState.sources) return
        request?.cancel()
        val previous = viewState
        viewState =
            if (source == previous.source) previous.copy(failed = false, loading = false)
            else ViewState(
                country = previous.country,
                sources = previous.sources,
                source = source,
                details = previous.details.copy(locationPoint = null),
                nameEdited = previous.nameEdited,
                isEditing = previous.isEditing,
            )
        next()
        if (source.access == SourceAccess.LIVE && !viewState.loaded) refresh()
    }

    private fun next() {
        val snapshot = viewState
        if (!snapshot.canContinue) return
        val next = snapshot.steps.getOrNull(snapshot.steps.indexOf(snapshot.step) + 1) ?: return
        val generatedName = when (snapshot.source?.access) {
            SourceAccess.LIVE -> snapshot.query.trim()
            SourceAccess.ADDRESS -> listOf(
                snapshot.cityQuery,
                snapshot.streetQuery,
                snapshot.house.trim()
            )
                .filter { it.isNotBlank() }.joinToString(" · ")

            else -> snapshot.source?.name.orEmpty().substringBefore(" · ")
        }
        viewState = snapshot.copy(
            step = next, failed = false, options = emptyList(),
            details = snapshot.details.copy(
                name = if (next == InternationalStep.Details && !snapshot.nameEdited) generatedName else snapshot.details.name,
                locationPoint = if (snapshot.step == InternationalStep.Location)
                    snapshot.outages.firstOrNull { it.location == snapshot.query.trim() && it.point != null }?.point
                    else snapshot.details.locationPoint,
            ),
        )
    }

    /** Returns false when the previous step is the shared country picker. */
    private fun back(): Boolean {
        val snapshot = viewState
        if (snapshot.saving) return true
        val previous =
            snapshot.steps.getOrNull(snapshot.steps.indexOf(snapshot.step) - 1) ?: return false
        request?.cancel()
        viewState =
            snapshot.copy(step = previous, options = emptyList(), loading = false, failed = false)
        return true
    }

    private fun detailsEvent(event: DetailsEvent) {
        val snapshot = viewState
        if (snapshot.saving) return
        viewState = when (event) {
            is DetailsEvent.AddressNameChanged -> snapshot.copy(
                details = snapshot.details.copy(name = event.name),
                nameEdited = true
            )

            is DetailsEvent.IconSelected -> snapshot.copy(details = snapshot.details.copy(iconId = event.icon.id))
            is DetailsEvent.QuietNightChanged -> snapshot.copy(details = snapshot.details.copy(quietNight = event.enabled))
            DetailsEvent.OpenReminder -> snapshot.copy(showReminderPicker = true)
            DetailsEvent.DismissReminder -> snapshot.copy(showReminderPicker = false)
            is DetailsEvent.ReminderSelected -> snapshot.copy(
                details = snapshot.details.copy(
                    reminderMinutes = event.minutes
                ), showReminderPicker = false
            )

            else -> snapshot
        }
    }

    private fun save() {
        val snapshot = viewState
        if (!snapshot.canSave) return
        val source = snapshot.source ?: return
        request?.cancel()
        viewState = snapshot.copy(saving = true, loading = false, failed = false)
        request = viewModelScope.launch {
            try {
                selections.save(
                    Selection.ByGroup(
                        region = Region("international:${source.id}", source.name, source.id),
                        group = OutageGroup(
                            snapshot.query.trim().ifBlank { snapshot.cityQuery },
                            source.name
                        ),
                        details = snapshot.details.copy(
                            name = snapshot.details.name.trim(),
                            country = snapshot.country,
                            sourceId = source.id,
                            filter = snapshot.query.trim(),
                            cityId = snapshot.city?.id,
                            streetId = snapshot.street?.id,
                            house = snapshot.house.trim(),
                        ),
                    ), editingPlaceId
                )
                viewState = viewState.copy(saving = false)
                sendAction(Action.Saved)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                viewState = viewState.copy(saving = false, failed = true)
            }
        }
    }

    private fun query(value: String) {
        viewState = viewState.copy(query = value)
    }

    private fun cityQuery(value: String) {
        request?.cancel()
        viewState = viewState.copy(
            cityQuery = value,
            city = null,
            street = null,
            streetQuery = "",
            house = "",
            options = emptyList(),
            outages = emptyList(),
            loaded = false,
            loading = false,
            failed = false,
        )
    }

    private fun streetQuery(value: String) {
        request?.cancel()
        viewState = viewState.copy(
            streetQuery = value, street = null, house = "", options = emptyList(),
            outages = emptyList(), loaded = false, loading = false, failed = false,
        )
    }

    private fun house(value: String) {
        viewState =
            viewState.copy(house = value, outages = emptyList(), loaded = false, failed = false)
    }

    private fun choose(option: AddressOption) {
        val snapshot = viewState
        if (option !in snapshot.options) return
        viewState = when (snapshot.step) {
            InternationalStep.Street -> snapshot.copy(street = option, streetQuery = option.name)
            InternationalStep.City -> snapshot.copy(
                city = option,
                cityQuery = option.name,
                street = null,
                streetQuery = "",
                house = ""
            )

            else -> return
        }
        next()
    }

    private fun search(street: Boolean) {
        val snapshot = viewState
        if (snapshot.step != if (street) InternationalStep.Street else InternationalStep.City) return
        val query = (if (street) snapshot.streetQuery else snapshot.cityQuery).trim()
        if (query.length < 3 || (street && snapshot.city == null)) return
        request?.cancel()
        viewState =
            snapshot.copy(loading = true, failed = false, loaded = false, options = emptyList())
        request = viewModelScope.launch {
            try {
                val options = if (street) repository.streets(
                    snapshot.city!!.id,
                    query
                ) else repository.cities(query)
                currentCoroutineContext().ensureActive()
                viewState = viewState.copy(options = options, loading = false, loaded = true)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                viewState = viewState.copy(loading = false, failed = true)
            }
        }
    }

    private fun refresh() {
        val snapshot = viewState
        val source = snapshot.source ?: return
        if (source.access != SourceAccess.LIVE) return
        request?.cancel()
        viewState =
            snapshot.copy(loading = true, failed = false, loaded = false, outages = emptyList())
        request = viewModelScope.launch {
            try {
                val outages = repository.outages(source)
                currentCoroutineContext().ensureActive()
                viewState = viewState.copy(outages = outages, loading = false, loaded = true)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                viewState = viewState.copy(loading = false, failed = true)
            }
        }
    }
}
