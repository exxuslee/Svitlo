package org.exxlexxlee.svitlo.ui.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.exxlexxlee.domain.model.Dials
import org.exxlexxlee.domain.model.KyivTime
import org.exxlexxlee.domain.model.OutageSchedule
import org.exxlexxlee.domain.model.Selection
import org.exxlexxlee.domain.usecase.ObserveDials
import org.exxlexxlee.domain.usecase.ObserveSchedule
import org.exxlexxlee.domain.usecase.ObserveSelection
import org.exxlexxlee.domain.usecase.RefreshSchedule
import kotlin.time.Duration.Companion.hours

data class ScheduleUiState(
    val selection: Selection? = null,
    val dials: Dials? = null,
    val schedule: OutageSchedule? = null,
    val isRefreshing: Boolean = false,
    val isStale: Boolean = false,
    val error: String? = null,
    val currentHour: Int = KyivTime.hourOfDay(),
    val loaded: Boolean = false,
)

@OptIn(ExperimentalCoroutinesApi::class)
class ScheduleViewModel(
    observeSelection: ObserveSelection,
    observeDials: ObserveDials,
    observeSchedule: ObserveSchedule,
    private val refreshSchedule: RefreshSchedule,
    private val clock: Clock = Clock.System,
) : ViewModel() {

    private val refreshState = MutableStateFlow(RefreshUi(isRefreshing = false, error = null))

    val state: StateFlow<ScheduleUiState> = combine(
        observeSelection(),
        refreshState,
    ) { selection, refresh -> selection to refresh }
        .flatMapLatest { (selection, refresh) ->
            if (selection == null) {
                flowOf(
                    ScheduleUiState(
                        loaded = true,
                        isRefreshing = refresh.isRefreshing,
                        error = refresh.error,
                        currentHour = KyivTime.hourOfDay(clock),
                    ),
                )
            } else {
                combine(
                    observeDials(selection),
                    observeSchedule(selection),
                ) { dials, schedule ->
                    ScheduleUiState(
                        selection = selection,
                        dials = dials,
                        schedule = schedule,
                        isRefreshing = refresh.isRefreshing,
                        isStale = isStale(schedule?.sourceUpdatedAt ?: schedule?.fetchedAt),
                        error = refresh.error,
                        currentHour = KyivTime.hourOfDay(clock),
                        loaded = true,
                    )
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ScheduleUiState())

    init {
        viewModelScope.launch {
            observeSelection().collect { selection ->
                if (selection != null) refresh(selection)
            }
        }
    }

    fun refresh(selection: Selection? = state.value.selection) {
        selection ?: return
        viewModelScope.launch {
            refreshState.update { it.copy(isRefreshing = true, error = null) }
            val result = refreshSchedule(selection)
            refreshState.update {
                it.copy(
                    isRefreshing = false,
                    error = result.exceptionOrNull()?.message,
                )
            }
        }
    }

    private fun isStale(updatedAt: Instant?): Boolean {
        if (updatedAt == null) return true
        return clock.now() - updatedAt > 24.hours
    }

    private data class RefreshUi(
        val isRefreshing: Boolean,
        val error: String?,
    )
}
