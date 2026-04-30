package com.timetrack.app.service

import com.timetrack.app.domain.model.TimerState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object TimerStateBus {
    private val _state = MutableSharedFlow<TimerState>(replay = 1)
    val state: SharedFlow<TimerState> = _state.asSharedFlow()

    suspend fun emit(state: TimerState) = _state.emit(state)
}
