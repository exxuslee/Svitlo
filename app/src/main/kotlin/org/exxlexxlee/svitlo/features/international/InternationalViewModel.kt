package org.exxlexxlee.svitlo.features.international

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import org.exxlexxlee.domain.model.AddressDetails
import org.exxlexxlee.domain.model.AddressOption
import org.exxlexxlee.domain.model.Country
import org.exxlexxlee.domain.model.InternationalOutage
import org.exxlexxlee.domain.model.OutageGroup
import org.exxlexxlee.domain.model.OutageSource
import org.exxlexxlee.domain.model.Region
import org.exxlexxlee.domain.model.Selection
import org.exxlexxlee.domain.model.SourceAccess
import org.exxlexxlee.domain.repository.InternationalOutageRepository
import org.exxlexxlee.domain.repository.SelectionRepository
import org.exxlexxlee.svitlo.features.address.models.Event
import java.time.Instant

enum class InternationalStep { Supplier, Location, City, Street, House, Details }

private fun SourceAccess?.steps(): List<InternationalStep> =
    listOf(InternationalStep.Supplier) + when (this) {
        SourceAccess.LIVE -> listOf(InternationalStep.Location)
        SourceAccess.ADDRESS -> listOf(
            InternationalStep.City,
            InternationalStep.Street,
            InternationalStep.House
        )

        else -> emptyList()
    } + InternationalStep.Details

data class InternationalState(
    val country: Country,
    val now: Instant = Instant.now(),
    val sources: List<OutageSource> = emptyList(),
    val source: OutageSource? = null,
    val step: InternationalStep = InternationalStep.Supplier,
    val outages: List<InternationalOutage> = emptyList(),
    val query: String = "",
    val cityQuery: String = "",
    val streetQuery: String = "",
    val house: String = "",
    val city: AddressOption? = null,
    val street: AddressOption? = null,
    val options: List<AddressOption> = emptyList(),
    val loading: Boolean = false,
    val saving: Boolean = false,
    val failed: Boolean = false,
    val loaded: Boolean = false,
    val details: AddressDetails = AddressDetails(name = ""),
    val nameEdited: Boolean = false,
    val showReminderPicker: Boolean = false,
    val isEditing: Boolean = false,
) {
    val steps: List<InternationalStep>
        get() = (source?.access ?: sources.maxByOrNull { it.access.steps().size }?.access).steps()
    val stepIndex: Int get() = steps.indexOf(step) + 2 // Country is the first step.
    val stepCount: Int get() = steps.size + 1
    val canContinue: Boolean
        get() = !saving && when (step) {
            InternationalStep.Supplier -> source != null
            InternationalStep.Location -> query.isNotBlank()
            InternationalStep.City -> city != null
            InternationalStep.Street -> street != null
            InternationalStep.House -> city != null && street != null && house.isNotBlank()
            InternationalStep.Details -> false
        }
    val canSave: Boolean
        get() = !saving && step == InternationalStep.Details &&
                source != null && details.name.isNotBlank() && when (source.access) {
            SourceAccess.LIVE -> query.isNotBlank()
            SourceAccess.ADDRESS -> city != null && street != null && house.isNotBlank()
            else -> true
        }
}

class InternationalViewModel(
    private val selections: SelectionRepository,
    private val repository: InternationalOutageRepository,
    savedStateHandle: SavedStateHandle = SavedStateHandle(),
) : ViewModel() {
    private val mutable = MutableStateFlow(InternationalState(Country.UKRAINE))
    val state = mutable.asStateFlow()
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
                mutable.value = InternationalState(
                    country = details.country, sources = sources, source = source,
                    step = InternationalStep.Details, query = details.filter,
                    city = details.cityId?.let { AddressOption(it, "") },
                    street = details.streetId?.let { AddressOption(it, "") },
                    house = details.house, details = details, nameEdited = true, isEditing = true,
                )
            }
        }
    }

    fun selectCountry(country: Country) {
        request?.cancel()
        mutable.value = InternationalState(country = country, sources = repository.sources(country))
    }

    fun select(source: OutageSource) {
        if (state.value.saving || source !in state.value.sources) return
        request?.cancel()
        val previous = state.value
        mutable.value =
            if (source == previous.source) previous.copy(failed = false, loading = false)
            else InternationalState(
                country = previous.country,
                sources = previous.sources,
                source = source,
                details = previous.details.copy(locationPoint = null),
                nameEdited = previous.nameEdited,
                isEditing = previous.isEditing,
            )
        next()
        if (source.access == SourceAccess.LIVE && !state.value.loaded) refresh()
    }

    fun next() {
        val snapshot = state.value
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
        mutable.value = snapshot.copy(
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
    fun back(): Boolean {
        val snapshot = state.value
        if (snapshot.saving) return true
        val previous =
            snapshot.steps.getOrNull(snapshot.steps.indexOf(snapshot.step) - 1) ?: return false
        request?.cancel()
        mutable.value =
            snapshot.copy(step = previous, options = emptyList(), loading = false, failed = false)
        return true
    }

    fun detailsEvent(event: Event) {
        val snapshot = state.value
        if (snapshot.saving) return
        mutable.value = when (event) {
            is Event.AddressNameChanged -> snapshot.copy(
                details = snapshot.details.copy(name = event.name),
                nameEdited = true
            )

            is Event.IconSelected -> snapshot.copy(details = snapshot.details.copy(iconId = event.icon.id))
            is Event.QuietNightChanged -> snapshot.copy(details = snapshot.details.copy(quietNight = event.enabled))
            Event.OpenReminder -> snapshot.copy(showReminderPicker = true)
            Event.DismissReminder -> snapshot.copy(showReminderPicker = false)
            is Event.ReminderSelected -> snapshot.copy(
                details = snapshot.details.copy(
                    reminderMinutes = event.minutes
                ), showReminderPicker = false
            )

            else -> snapshot
        }
    }

    fun save(onSaved: () -> Unit) {
        val snapshot = state.value
        if (!snapshot.canSave) return
        val source = snapshot.source ?: return
        request?.cancel()
        mutable.value = snapshot.copy(saving = true, loading = false, failed = false)
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
                mutable.value = state.value.copy(saving = false)
                onSaved()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                mutable.value = state.value.copy(saving = false, failed = true)
            }
        }
    }

    fun query(value: String) {
        mutable.value = state.value.copy(query = value)
    }

    fun cityQuery(value: String) {
        request?.cancel()
        mutable.value = state.value.copy(
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

    fun streetQuery(value: String) {
        request?.cancel()
        mutable.value = state.value.copy(
            streetQuery = value, street = null, house = "", options = emptyList(),
            outages = emptyList(), loaded = false, loading = false, failed = false,
        )
    }

    fun house(value: String) {
        mutable.value =
            state.value.copy(house = value, outages = emptyList(), loaded = false, failed = false)
    }

    fun choose(option: AddressOption) {
        val snapshot = state.value
        if (option !in snapshot.options) return
        mutable.value = when (snapshot.step) {
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

    fun search(street: Boolean) {
        val snapshot = state.value
        if (snapshot.step != if (street) InternationalStep.Street else InternationalStep.City) return
        val query = (if (street) snapshot.streetQuery else snapshot.cityQuery).trim()
        if (query.length < 3 || (street && snapshot.city == null)) return
        request?.cancel()
        mutable.value =
            snapshot.copy(loading = true, failed = false, loaded = false, options = emptyList())
        request = viewModelScope.launch {
            try {
                val options = if (street) repository.streets(
                    snapshot.city!!.id,
                    query
                ) else repository.cities(query)
                currentCoroutineContext().ensureActive()
                mutable.value = state.value.copy(options = options, loading = false, loaded = true)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                mutable.value = state.value.copy(loading = false, failed = true)
            }
        }
    }

    fun refresh() {
        val snapshot = state.value
        val source = snapshot.source ?: return
        if (source.access != SourceAccess.LIVE) return
        request?.cancel()
        mutable.value =
            snapshot.copy(loading = true, failed = false, loaded = false, outages = emptyList())
        request = viewModelScope.launch {
            try {
                val outages = repository.outages(source)
                currentCoroutineContext().ensureActive()
                mutable.value = state.value.copy(outages = outages, loading = false, loaded = true)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                mutable.value = state.value.copy(loading = false, failed = true)
            }
        }
    }
}
