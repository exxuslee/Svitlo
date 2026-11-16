package org.exxlexxlee.svitlo.features.international

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.exxlexxlee.domain.model.Country
import org.exxlexxlee.domain.model.OutageAddress
import org.exxlexxlee.domain.model.Place
import org.exxlexxlee.domain.model.SourceAccess
import org.exxlexxlee.domain.repository.InternationalOutageRepository
import org.exxlexxlee.domain.repository.SelectionRepository
import org.exxlexxlee.svitlo.ui.schedule.ScheduleCardUi
import java.time.Duration
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import org.exxlexxlee.domain.model.OutageCardState

class SavedInternationalViewModel(
    selections: SelectionRepository,
    private val repository: InternationalOutageRepository,
    private val clock: java.time.Clock = java.time.Clock.systemUTC(),
) : ViewModel() {
    private var places: List<Place> = emptyList()
    private var job: Job? = null
    private val mutable = MutableStateFlow<Map<Long, ScheduleCardUi>>(emptyMap())
    val cards = mutable.asStateFlow()

    init {
        viewModelScope.launch {
            var elapsed = 0
            while (isActive) {
                delay(30_000)
                val now = clock.instant()
                mutable.value = mutable.value.mapValues { (_, card) -> card.copy(now = now) }
                elapsed += 30
                // Incident feeds change more often than day schedules.
                if (elapsed >= 300) { elapsed = 0; refresh() }
            }
        }
        viewModelScope.launch {
            selections.observePlaces().collect { all ->
                val incoming =
                    all.filter { it.selection.details?.country?.let { country -> country != Country.UKRAINE } == true }
                val unchanged = incoming.filter { it in places }.map { it.id }.toSet()
                mutable.value = mutable.value.filterKeys { it in unchanged }
                places = incoming
                refresh()
            }
        }
    }

    fun refresh() {
        job?.cancel()
        val current = places
        mutable.value =
            current.mapNotNull { place -> (mutable.value[place.id] ?: placeholder(place))?.let { place.id to it } }.toMap()
        job = viewModelScope.launch {
            for (place in current) {
                val details = place.selection.details ?: continue
                val source = repository.sources(details.country).find { it.id == details.sourceId }
                    ?: continue
                if (source.access !in listOf(SourceAccess.LIVE, SourceAccess.ADDRESS)) {
                    mutable.value[place.id]?.let { card ->
                        mutable.value = mutable.value + (place.id to card.copy(outageState = OutageCardState.Error))
                    }
                    continue
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
                    val events = repository.outages(source, address).filter {
                        (it.location + " " + it.description).contains(
                            details.filter,
                            true
                        )
                    }
                    currentCoroutineContext().ensureActive()
                    val now = clock.instant()
                    mutable.value = mutable.value + (place.id to InternationalCardMapper.map(
                        events,
                        source,
                        now,
                        details.filter.ifBlank { details.name },
                        details.locationPoint,
                    ).copy(id = place.id, title = details.name, iconId = details.iconId))
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    val card = mutable.value[place.id] ?: continue
                    val cached = when (val state = card.outageState) {
                        is OutageCardState.Content -> state.snapshot
                        is OutageCardState.Stale -> state.snapshot
                        else -> null
                    }
                    mutable.value = mutable.value + (place.id to card.copy(
                        now = clock.instant(),
                        outageState = cached?.let { OutageCardState.Stale(it,
                            Duration.between(it.fetchedAt, clock.instant()).coerceAtLeast(Duration.ZERO)) } ?: OutageCardState.Error,
                    ))
                }
            }
        }
    }

    private fun placeholder(place: Place): ScheduleCardUi? {
        val details = place.selection.details ?: return null
        val source =
            repository.sources(details.country).find { it.id == details.sourceId } ?: return null
        return InternationalCardMapper.map(
            emptyList(), source, clock.instant(), details.name,
        ).copy(id = place.id, title = details.name, iconId = details.iconId, outageState = OutageCardState.Loading)
    }
}
