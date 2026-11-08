package org.exxlexxlee.svitlo.features.schedule

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.exxlexxlee.domain.model.KyivTime
import org.exxlexxlee.domain.model.Selection
import org.exxlexxlee.domain.usecase.ObserveDials
import org.exxlexxlee.domain.usecase.ObserveSchedule
import org.exxlexxlee.domain.usecase.ObserveSelection
import org.exxlexxlee.domain.usecase.RefreshSchedule
import org.exxlexxlee.svitlo.features.schedule.models.Action
import org.exxlexxlee.svitlo.features.schedule.models.Event
import org.exxlexxlee.svitlo.features.schedule.models.ViewState
import org.exxlexxlee.svitlo.ui.common.BaseViewModel
import kotlin.time.Duration.Companion.hours

@OptIn(ExperimentalCoroutinesApi::class)
class ScheduleViewModel(
    observeSelection: ObserveSelection,
    observeDials: ObserveDials,
    observeSchedule: ObserveSchedule,
    private val refreshSchedule: RefreshSchedule,
    private val clock: Clock,
) : BaseViewModel<ViewState, Action, Event>(
    initialState = ViewState(currentHour = KyivTime.hourOfDay(clock))
) {
    private val refreshState = MutableStateFlow(RefreshUi(isRefreshing = false, error = null))

    init {
        viewModelScope.launch {
            combine(
                observeSelection(),
                refreshState,
            ) { selection, refresh -> selection to refresh }
                .flatMapLatest { (selection, refresh) ->
                    if (selection == null) {
                        flowOf(
                            ViewState(
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
                            ViewState(
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
                .collect { viewState = it }
        }
        viewModelScope.launch {
            observeSelection().collect { selection ->
                if (selection != null) refresh(selection)
            }
        }
    }

    override fun obtainEvent(viewEvent: Event) {
        when (viewEvent) {
            Event.Refresh -> refresh(viewState.selection)
            Event.ChangeSelection -> viewAction = Action.OpenAddress
        }
    }

    private fun refresh(selection: Selection? = viewState.selection) {
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
