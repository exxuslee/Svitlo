package org.exxlexxlee.svitlo.features.home

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.exxlexxlee.domain.model.AddressDetails
import org.exxlexxlee.domain.model.OutageGroup
import org.exxlexxlee.domain.model.OutageSchedule
import org.exxlexxlee.domain.model.Place
import org.exxlexxlee.domain.model.Region
import org.exxlexxlee.domain.model.Selection
import org.exxlexxlee.domain.repository.ScheduleRepository
import org.exxlexxlee.domain.repository.SelectionRepository
import org.exxlexxlee.domain.usecase.DeletePlace
import org.exxlexxlee.domain.usecase.ObservePlaces
import org.exxlexxlee.domain.usecase.ObserveSchedule
import org.exxlexxlee.domain.usecase.RefreshSchedule
import org.exxlexxlee.svitlo.features.home.models.Action
import org.exxlexxlee.svitlo.features.home.models.Event
import org.exxlexxlee.svitlo.widget.WidgetManager
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val placesFlow = MutableStateFlow<List<Place>>(emptyList())
    private val scheduleFlow = MutableStateFlow<OutageSchedule?>(null)

    private val fakeSelectionRepository = object : SelectionRepository {
        override fun observePlaces(): Flow<List<Place>> = placesFlow
        override fun observePlace(id: Long): Flow<Place?> = MutableStateFlow(null)
        override fun observe(): Flow<Selection?> = MutableStateFlow(null)
        override suspend fun save(selection: Selection, placeId: Long?): Long = 1L
        override suspend fun delete(id: Long) {}
    }

    private val fakeScheduleRepository = object : ScheduleRepository {
        override fun observe(selection: Selection): Flow<OutageSchedule?> = scheduleFlow
        override suspend fun refresh(selection: Selection): Result<Unit> = Result.success(Unit)
    }

    private class FakeWidgetManager(
        var hasWidgets: Boolean = false,
        var isDismissed: Boolean = false,
    ) : WidgetManager {
        var requestedPin = false

        override fun hasInstalledWidgets(): Boolean = hasWidgets
        override fun isBannerDismissed(): Boolean = isDismissed
        override fun setBannerDismissed(dismissed: Boolean) {
            isDismissed = dismissed
        }
        override fun requestPinWidget(): Boolean {
            requestedPin = true
            return true
        }
    }

    private val fixedClock = object : Clock {
        override fun now(): Instant = Instant.parse("2026-11-09T10:00:00Z")
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(widgetManager: WidgetManager): HomeViewModel {
        return HomeViewModel(
            observePlaces = ObservePlaces(fakeSelectionRepository),
            observeSchedule = ObserveSchedule(fakeScheduleRepository),
            refreshSchedule = RefreshSchedule(fakeScheduleRepository),
            deletePlace = DeletePlace(fakeSelectionRepository),
            clock = fixedClock,
            widgetManager = widgetManager,
        )
    }

    private fun createPlace(id: Long = 1): Place {
        return Place(
            id = id,
            selection = Selection.ByGroup(
                region = Region(id = "kyiv-region", name = "Київська область", providerId = "dtek"),
                group = OutageGroup(id = "2.2", name = "2.2"),
                details = AddressDetails(name = "Київська область"),
            ),
        )
    }

    @Test
    fun widgetBanner_shownWhenCardsExistAndWidgetNotInstalled() = runTest(testDispatcher) {
        val widgetManager = FakeWidgetManager(hasWidgets = false, isDismissed = false)
        val viewModel = createViewModel(widgetManager)

        placesFlow.value = listOf(createPlace())
        advanceUntilIdle()

        val state = viewModel.viewStates().value
        assertEquals(1, state.cards.size)
        assertTrue(state.showWidgetBanner)
    }

    @Test
    fun widgetBanner_notShownWhenCardsEmpty() = runTest(testDispatcher) {
        val widgetManager = FakeWidgetManager(hasWidgets = false, isDismissed = false)
        val viewModel = createViewModel(widgetManager)

        placesFlow.value = emptyList()
        advanceUntilIdle()

        val state = viewModel.viewStates().value
        assertEquals(0, state.cards.size)
        assertFalse(state.showWidgetBanner)
    }

    @Test
    fun widgetBanner_notShownWhenWidgetAlreadyInstalled() = runTest(testDispatcher) {
        val widgetManager = FakeWidgetManager(hasWidgets = true, isDismissed = false)
        val viewModel = createViewModel(widgetManager)

        placesFlow.value = listOf(createPlace())
        advanceUntilIdle()

        val state = viewModel.viewStates().value
        assertEquals(1, state.cards.size)
        assertFalse(state.showWidgetBanner)
    }

    @Test
    fun widgetBanner_notShownWhenBannerDismissed() = runTest(testDispatcher) {
        val widgetManager = FakeWidgetManager(hasWidgets = false, isDismissed = true)
        val viewModel = createViewModel(widgetManager)

        placesFlow.value = listOf(createPlace())
        advanceUntilIdle()

        val state = viewModel.viewStates().value
        assertEquals(1, state.cards.size)
        assertFalse(state.showWidgetBanner)
    }

    @Test
    fun addWidgetClicked_emitsRequestPinWidgetAction() = runTest(testDispatcher) {
        val widgetManager = FakeWidgetManager(hasWidgets = false, isDismissed = false)
        val viewModel = createViewModel(widgetManager)

        placesFlow.value = listOf(createPlace())
        advanceUntilIdle()

        viewModel.obtainEvent(Event.AddWidgetClicked)

        val action = viewModel.viewActions().first()
        assertEquals(Action.RequestPinWidget, action)
    }

    @Test
    fun dismissBanner_savesDismissedAndHidesBanner() = runTest(testDispatcher) {
        val widgetManager = FakeWidgetManager(hasWidgets = false, isDismissed = false)
        val viewModel = createViewModel(widgetManager)

        placesFlow.value = listOf(createPlace())
        advanceUntilIdle()

        assertTrue(viewModel.viewStates().value.showWidgetBanner)

        viewModel.obtainEvent(Event.DismissWidgetBanner)

        assertTrue(widgetManager.isDismissed)
        assertFalse(viewModel.viewStates().value.showWidgetBanner)
    }

    @Test
    fun checkWidget_updatesBannerWhenWidgetIsAdded() = runTest(testDispatcher) {
        val widgetManager = FakeWidgetManager(hasWidgets = false, isDismissed = false)
        val viewModel = createViewModel(widgetManager)

        placesFlow.value = listOf(createPlace())
        advanceUntilIdle()

        assertTrue(viewModel.viewStates().value.showWidgetBanner)

        widgetManager.hasWidgets = true
        viewModel.obtainEvent(Event.CheckWidget)

        assertFalse(viewModel.viewStates().value.showWidgetBanner)
    }
}
