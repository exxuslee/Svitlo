package org.exxlexxlee.svitlo.features.home

import androidx.lifecycle.ViewModelStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import org.exxlexxlee.domain.model.AddressDetails
import org.exxlexxlee.domain.model.DaySchedule
import org.exxlexxlee.domain.model.OutageGroup
import org.exxlexxlee.domain.model.OutageSchedule
import org.exxlexxlee.domain.model.Place
import org.exxlexxlee.domain.model.PowerState
import org.exxlexxlee.domain.model.Region
import org.exxlexxlee.domain.model.Selection
import org.exxlexxlee.domain.repository.ScheduleRepository
import org.exxlexxlee.domain.repository.SelectionRepository
import org.exxlexxlee.domain.usecase.DeletePlace
import org.exxlexxlee.domain.usecase.ObservePlaces
import org.exxlexxlee.domain.usecase.ObservePlaceSchedules
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
    private val viewModelStore = ViewModelStore()
    private val placesFlow = MutableStateFlow<List<Place>>(emptyList())
    private val scheduleFlow = MutableStateFlow<OutageSchedule?>(null)
    private var placeCollections = 0
    private val refreshedSelections = mutableListOf<Selection>()

    private val fakeSelectionRepository = object : SelectionRepository {
        override fun observePlaces(): Flow<List<Place>> = flow {
            placeCollections += 1
            emitAll(placesFlow)
        }
        override fun observePlace(id: Long): Flow<Place?> = MutableStateFlow(null)
        override fun observe(): Flow<Selection?> = MutableStateFlow(null)
        override suspend fun save(selection: Selection, placeId: Long?): Long = 1L
        override suspend fun delete(id: Long) {}
    }

    private val fakeScheduleRepository = object : ScheduleRepository {
        override fun observe(selection: Selection): Flow<OutageSchedule?> = scheduleFlow
        override suspend fun refresh(selection: Selection): Result<Unit> {
            refreshedSelections += selection
            return Result.success(Unit)
        }
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
        viewModelStore.clear()
        Dispatchers.resetMain()
    }

    private fun runViewModelTest(block: suspend TestScope.() -> Unit) = runTest(testDispatcher) {
        try {
            block()
        } finally {
            viewModelStore.clear()
        }
    }

    private fun createViewModel(widgetManager: WidgetManager): HomeViewModel {
        val viewModel = HomeViewModel(
            observePlaceSchedules = ObservePlaceSchedules(
                ObservePlaces(fakeSelectionRepository),
                ObserveSchedule(fakeScheduleRepository),
            ),
            refreshSchedule = RefreshSchedule(fakeScheduleRepository),
            deletePlace = DeletePlace(fakeSelectionRepository),
            clock = fixedClock,
            widgetManager = widgetManager,
        )
        viewModelStore.put("home", viewModel)
        return viewModel
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
    fun widgetBanner_shownWhenCardsExistAndWidgetNotInstalled() = runViewModelTest {
        val widgetManager = FakeWidgetManager(hasWidgets = false, isDismissed = false)
        val viewModel = createViewModel(widgetManager)

        placesFlow.value = listOf(createPlace())
        runCurrent()

        val state = viewModel.viewStates().value
        assertEquals(1, state.cards.size)
        assertTrue(state.showWidgetBanner)
    }

    @Test
    fun widgetBanner_notShownWhenCardsEmpty() = runViewModelTest {
        val widgetManager = FakeWidgetManager(hasWidgets = false, isDismissed = false)
        val viewModel = createViewModel(widgetManager)

        placesFlow.value = emptyList()
        runCurrent()

        val state = viewModel.viewStates().value
        assertEquals(0, state.cards.size)
        assertFalse(state.showWidgetBanner)
    }

    @Test
    fun widgetBanner_notShownWhenWidgetAlreadyInstalled() = runViewModelTest {
        val widgetManager = FakeWidgetManager(hasWidgets = true, isDismissed = false)
        val viewModel = createViewModel(widgetManager)

        placesFlow.value = listOf(createPlace())
        runCurrent()

        val state = viewModel.viewStates().value
        assertEquals(1, state.cards.size)
        assertFalse(state.showWidgetBanner)
    }

    @Test
    fun widgetBanner_notShownWhenBannerDismissed() = runViewModelTest {
        val widgetManager = FakeWidgetManager(hasWidgets = false, isDismissed = true)
        val viewModel = createViewModel(widgetManager)

        placesFlow.value = listOf(createPlace())
        runCurrent()

        val state = viewModel.viewStates().value
        assertEquals(1, state.cards.size)
        assertFalse(state.showWidgetBanner)
    }

    @Test
    fun addWidgetClicked_emitsRequestPinWidgetAction() = runViewModelTest {
        val widgetManager = FakeWidgetManager(hasWidgets = false, isDismissed = false)
        val viewModel = createViewModel(widgetManager)

        placesFlow.value = listOf(createPlace())
        runCurrent()

        viewModel.obtainEvent(Event.AddWidgetClicked)

        val action = viewModel.viewActions().first()
        assertEquals(Action.RequestPinWidget, action)
    }

    @Test
    fun dismissBanner_savesDismissedAndHidesBanner() = runViewModelTest {
        val widgetManager = FakeWidgetManager(hasWidgets = false, isDismissed = false)
        val viewModel = createViewModel(widgetManager)

        placesFlow.value = listOf(createPlace())
        runCurrent()

        assertTrue(viewModel.viewStates().value.showWidgetBanner)

        viewModel.obtainEvent(Event.DismissWidgetBanner)

        assertTrue(widgetManager.isDismissed)
        assertFalse(viewModel.viewStates().value.showWidgetBanner)
    }

    @Test
    fun checkWidget_updatesBannerWhenWidgetIsAdded() = runViewModelTest {
        val widgetManager = FakeWidgetManager(hasWidgets = false, isDismissed = false)
        val viewModel = createViewModel(widgetManager)

        placesFlow.value = listOf(createPlace())
        runCurrent()

        assertTrue(viewModel.viewStates().value.showWidgetBanner)

        widgetManager.hasWidgets = true
        viewModel.obtainEvent(Event.CheckWidget)

        assertFalse(viewModel.viewStates().value.showWidgetBanner)
    }

    @Test
    fun scheduleUpdatesDoNotTriggerAnotherRefreshOrAnotherPlacesSubscription() = runViewModelTest {
        val place = createPlace()
        placesFlow.value = listOf(place)
        val viewModel = createViewModel(FakeWidgetManager())
        runCurrent()
        assertEquals(1, placeCollections)
        assertEquals(listOf(place.selection), refreshedSelections)

        scheduleFlow.value = OutageSchedule(
            group = place.selection.group,
            today = DaySchedule(LocalDate(2026, 11, 9), List(48) { PowerState.ON }),
            tomorrow = null,
            sourceUpdatedAt = null,
            fetchedAt = fixedClock.now(),
        )
        runCurrent()
        assertEquals(PowerState.ON, viewModel.viewStates().value.cards.single().currentState)
        assertEquals(1, refreshedSelections.size)
        assertEquals(1, placeCollections)

        viewModel.obtainEvent(Event.Refresh)
        runCurrent()
        assertEquals(2, refreshedSelections.size)
        assertFalse(viewModel.viewStates().value.isRefreshing)
    }

    @Test
    fun changedPlacesAreRefreshedAndDeletingLastPlaceClearsCards() = runViewModelTest {
        val original = createPlace()
        placesFlow.value = listOf(original)
        val viewModel = createViewModel(FakeWidgetManager())
        runCurrent()

        val selection = original.selection as Selection.ByGroup
        val edited = original.copy(selection = selection.copy(group = OutageGroup("3.1", "3.1")))
        placesFlow.value = listOf(edited)
        runCurrent()
        assertEquals(listOf(original.selection, edited.selection), refreshedSelections)
        assertEquals("Група 3.1", viewModel.viewStates().value.cards.single().groupLabel)

        placesFlow.value = emptyList()
        runCurrent()
        assertTrue(viewModel.viewStates().value.cards.isEmpty())
        assertFalse(viewModel.viewStates().value.isRefreshing)
        assertFalse(viewModel.viewStates().value.showWidgetBanner)
    }
}
