package org.exxlexxlee.domain.usecase

import kotlinx.coroutines.flow.Flow
import org.exxlexxlee.domain.model.Place
import org.exxlexxlee.domain.model.Selection
import org.exxlexxlee.domain.repository.SelectionRepository

class SaveSelection(private val selectionRepository: SelectionRepository) {
    suspend operator fun invoke(selection: Selection, placeId: Long? = null): Long =
        selectionRepository.save(selection, placeId)
}

class ObserveSelection(private val selectionRepository: SelectionRepository) {
    operator fun invoke(): Flow<Selection?> = selectionRepository.observe()
}

class ObservePlaces(private val selectionRepository: SelectionRepository) {
    operator fun invoke(): Flow<List<Place>> = selectionRepository.observePlaces()
}

class ObservePlace(private val selectionRepository: SelectionRepository) {
    operator fun invoke(id: Long): Flow<Place?> = selectionRepository.observePlace(id)
}

class DeletePlace(private val selectionRepository: SelectionRepository) {
    suspend operator fun invoke(id: Long) = selectionRepository.delete(id)
}
