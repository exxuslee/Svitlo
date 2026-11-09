package org.exxlexxlee.domain.repository

import kotlinx.coroutines.flow.Flow
import org.exxlexxlee.domain.model.Place
import org.exxlexxlee.domain.model.Selection

interface SelectionRepository {
    fun observePlaces(): Flow<List<Place>>
    fun observePlace(id: Long): Flow<Place?>
    fun observe(): Flow<Selection?>
    suspend fun save(selection: Selection, placeId: Long? = null): Long
    suspend fun delete(id: Long)
}
