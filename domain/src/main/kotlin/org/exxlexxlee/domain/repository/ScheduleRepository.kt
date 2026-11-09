package org.exxlexxlee.domain.repository

import kotlinx.coroutines.flow.Flow
import org.exxlexxlee.domain.model.OutageSchedule
import org.exxlexxlee.domain.model.Selection

interface ScheduleRepository {
    fun observe(selection: Selection): Flow<OutageSchedule?>
    suspend fun refresh(selection: Selection): Result<Unit>
    suspend fun refreshAll(selections: List<Selection>): Map<Selection, Result<Unit>> {
        val results = mutableMapOf<Pair<String, String>, Result<Unit>>()
        return selections.associateWith { selection ->
            results.getOrPut(selection.region.id to selection.group.id) { refresh(selection) }
        }
    }
}
