package com.timetrack.app

import app.cash.turbine.test
import com.timetrack.app.data.datastore.TimerStateStore
import com.timetrack.app.data.repository.SessionRepository
import com.timetrack.app.domain.model.TimerState
import com.timetrack.app.domain.usecase.GetTodayTotal
import com.timetrack.app.domain.usecase.StartTimer
import com.timetrack.app.domain.usecase.StopTimer
import com.timetrack.app.service.TimerStateBus
import com.timetrack.app.ui.screens.home.HomeViewModel
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    private val startTimer = mockk<StartTimer>(relaxed = true)
    private val stopTimer = mockk<StopTimer>(relaxed = true)
    private val getTodayTotal = mockk<GetTodayTotal>()
    private val sessionRepository = mockk<SessionRepository>()
    private val timerStateStore = mockk<TimerStateStore>()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        mockkObject(TimerStateBus)
        val fakeState = MutableSharedFlow<TimerState>(replay = 1, extraBufferCapacity = 1)
        fakeState.tryEmit(TimerState.Idle)
        every { TimerStateBus.state } returns fakeState
        every { getTodayTotal() } returns flowOf(0L)
        every { sessionRepository.getAll() } returns flowOf(emptyList())
        every { timerStateStore.isRunning } returns MutableStateFlow(false)
        every { timerStateStore.startTimeMs } returns MutableStateFlow(null)
        coEvery { TimerStateBus.emit(any()) } returns Unit
    }

    @After
    fun tearDown() {
        unmockkObject(TimerStateBus)
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is Idle`() = runTest {
        val viewModel = HomeViewModel(
            startTimer, stopTimer, getTodayTotal, sessionRepository, timerStateStore
        )
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(TimerState.Idle, state.timerState)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `todayTotalMs reflects getTodayTotal flow`() = runTest {
        every { getTodayTotal() } returns flowOf(3_600_000L)
        val viewModel = HomeViewModel(
            startTimer, stopTimer, getTodayTotal, sessionRepository, timerStateStore
        )
        viewModel.uiState.test {
            dispatcher.scheduler.advanceUntilIdle()
            val state = expectMostRecentItem()
            assertEquals(3_600_000L, state.todayTotalMs)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
