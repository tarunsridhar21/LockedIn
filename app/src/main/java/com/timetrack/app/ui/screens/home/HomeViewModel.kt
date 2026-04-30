package com.timetrack.app.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.timetrack.app.data.datastore.TimerStateStore
import com.timetrack.app.data.repository.SessionRepository
import com.timetrack.app.domain.model.Session
import com.timetrack.app.domain.model.TimerState
import com.timetrack.app.domain.usecase.GetTodayTotal
import com.timetrack.app.domain.usecase.StartTimer
import com.timetrack.app.domain.usecase.StopTimer
import com.timetrack.app.service.TimerStateBus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val timerState: TimerState = TimerState.Idle,
    val recentSessions: List<Session> = emptyList(),
    val todayTotalMs: Long = 0L,
    val lastSavedSessionId: Long? = null,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val startTimer: StartTimer,
    private val stopTimer: StopTimer,
    private val getTodayTotal: GetTodayTotal,
    private val sessionRepository: SessionRepository,
    private val timerStateStore: TimerStateStore,
) : ViewModel() {

    private val _lastSavedSessionId = MutableStateFlow<Long?>(null)

    val uiState: StateFlow<HomeUiState> = combine(
        TimerStateBus.state,
        sessionRepository.getAll(),
        getTodayTotal(),
        _lastSavedSessionId,
    ) { timerState, sessions, todayMs, lastId ->
        HomeUiState(
            timerState = timerState,
            recentSessions = sessions.take(5),
            todayTotalMs = todayMs,
            lastSavedSessionId = lastId,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState(),
    )

    init {
        // Restore timer state from DataStore on startup
        viewModelScope.launch {
            val isRunning = timerStateStore.isRunning.first()
            val startMs = timerStateStore.startTimeMs.first()
            if (isRunning && startMs != null) {
                TimerStateBus.emit(TimerState.Running(startMs))
            } else {
                TimerStateBus.emit(TimerState.Idle)
            }
        }
    }

    fun startTimer() = startTimer.invoke()

    fun stopTimer() = stopTimer.invoke()

    fun onSessionSaved(sessionId: Long) {
        _lastSavedSessionId.value = sessionId
    }

    fun clearLastSaved() {
        _lastSavedSessionId.value = null
    }
}
