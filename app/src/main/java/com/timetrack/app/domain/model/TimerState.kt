package com.timetrack.app.domain.model

sealed interface TimerState {
    data object Idle : TimerState
    data class Running(val startTimeMs: Long) : TimerState
    data object Saving : TimerState
}
