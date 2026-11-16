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
import org.exxlexxlee.domain.model.InternationalOutage
import org.exxlexxlee.domain.model.OutageAddress
import org.exxlexxlee.domain.model.Place
import org.exxlexxlee.domain.model.SourceAccess
import org.exxlexxlee.domain.repository.InternationalOutageRepository
import org.exxlexxlee.domain.repository.SelectionRepository
import org.exxlexxlee.svitlo.ui.schedule.ScheduleCardUi
import java.time.Instant

class SavedInternationalViewModel(
    selections: SelectionRepository,
    private val repository: InternationalOutageRepository,
) : ViewModel() {
    private var places: List<Place> = emptyList()
    private var job: Job? = null
    private val mutable = MutableStateFlow<Map<Long, ScheduleCardUi>>(emptyMap())
    val cards = mutable.asStateFlow()

    init {
        viewModelScope.launch {
            selections.observePlaces().collect { all ->
                places =
                    all.filter { it.selection.details?.country?.let { country -> country != Country.UKRAINE } == true }
                refresh()
            }
        }
    }

    fun refresh() {
        job?.cancel()
        val current = places
        mutable.value =
            current.mapNotNull { place -> placeholder(place)?.let { place.id to it } }.toMap()
        job = viewModelScope.launch {
            for (place in current) {
                val details = place.selection.details ?: continue
                val source = repository.sources(details.country).find { it.id == details.sourceId }
                    ?: continue
                if (source.access !in listOf(SourceAccess.LIVE, SourceAccess.ADDRESS)) continue
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
                    val records = events.ifEmpty {
                        listOf(
                            InternationalOutage(
                                "unknown",
                                details.name,
                                "",
                                null,
                                null
                            )
                        )
                    }
                    mutable.value = mutable.value + (place.id to InternationalCardMapper.map(
                        records,
                        source,
                        Instant.now()
                    ).copy(id = place.id, title = details.name, iconId = details.iconId))
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) { /* Keep unknown slots when the provider is unavailable. */
                }
            }
        }
    }

    private fun placeholder(place: Place): ScheduleCardUi? {
        val details = place.selection.details ?: return null
        val source =
            repository.sources(details.country).find { it.id == details.sourceId } ?: return null
        return InternationalCardMapper.map(
            listOf(
                InternationalOutage(
                    "unknown",
                    details.name,
                    "",
                    null,
                    null
                )
            ), source, Instant.now()
        ).copy(id = place.id, title = details.name, iconId = details.iconId)
    }
}
