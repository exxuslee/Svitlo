package org.exxlexxlee.svitlo.ui.common

import androidx.lifecycle.ViewModelStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BaseViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    private val viewModel = TestViewModel()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        store.put("test", viewModel)
    }

    @After
    fun tearDown() {
        store.clear()
        Dispatchers.resetMain()
    }

    @Test
    fun actionsWaitForConsumerAndKeepTheirOrder() = runTest(dispatcher) {
        // More actions than the buffer capacity must suspend producers rather than drop actions.
        repeat(100) { viewModel.obtainEvent(it) }
        runCurrent()
        val received = List(100) { viewModel.viewActions().first() }
        assertEquals((0 until 100).toList(), received)
    }

    @Test
    fun consumedActionsAreNotReplayedAfterResubscription() = runTest(dispatcher) {
        viewModel.obtainEvent(1)
        assertEquals(1, viewModel.viewActions().first())
        val received = mutableListOf<Int>()
        val consumer = backgroundScope.launch {
            viewModel.viewActions().collect { received += it }
        }
        runCurrent()
        assertEquals(emptyList<Int>(), received)
        consumer.cancel()
        runCurrent()

        // Simulate a paused screen followed by a new subscription.
        viewModel.obtainEvent(2)
        runCurrent()
        assertEquals(2, viewModel.viewActions().first())
    }

    private class TestViewModel : BaseViewModel<Unit, Int, Int>(Unit) {
        override fun obtainEvent(viewEvent: Int) = sendAction(viewEvent)
    }
}
