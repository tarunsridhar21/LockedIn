package com.timetrack.app.ui.screens.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.timetrack.app.data.repository.CategoryRepository
import com.timetrack.app.data.repository.SessionRepository
import com.timetrack.app.domain.model.Category
import com.timetrack.app.domain.model.Session
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class DayGroup(
    val label: String,
    val sessions: List<Session>,
    val totalMs: Long,
)

data class HistoryUiState(
    val dayGroups: List<DayGroup> = emptyList(),
    val categories: List<Category> = emptyList(),
    val activeFilter: Long? = null, // null=all, -1=unlabeled, else categoryId
    val unlabeledCount: Int = 0,
    val currentMonth: YearMonth = YearMonth.now(),
    val activityDays: Set<LocalDate> = emptySet(),
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val categoryRepository: CategoryRepository,
) : ViewModel() {

    private val _activeFilter = MutableStateFlow<Long?>(null)

    val uiState: StateFlow<HistoryUiState> = combine(
        sessionRepository.getAll(),
        categoryRepository.getAll(),
        _activeFilter,
    ) { sessions, categories, filter ->
        val filtered = when (filter) {
            null -> sessions
            -1L -> sessions.filter { it.category == null }
            else -> sessions.filter { it.category?.id == filter }
        }

        val zone = ZoneId.systemDefault()
        val yearMonth = YearMonth.now()
        val activityDays = sessions
            .filter { filter == null || (filter == -1L) == (it.category == null) || it.category?.id == filter }
            .map { Instant.ofEpochMilli(it.startTimeMs).atZone(zone).toLocalDate() }
            .filter { it.year == yearMonth.year && it.monthValue == yearMonth.monthValue }
            .toSet()

        HistoryUiState(
            dayGroups = groupByDay(filtered),
            categories = categories,
            activeFilter = filter,
            unlabeledCount = sessions.count { it.category == null },
            currentMonth = yearMonth,
            activityDays = activityDays,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HistoryUiState(),
    )

    fun setFilter(categoryId: Long?) {
        _activeFilter.value = categoryId
    }

    fun deleteSession(session: Session) {
        viewModelScope.launch {
            sessionRepository.delete(session)
        }
    }

    private fun groupByDay(sessions: List<Session>): List<DayGroup> {
        val today = startOfDay(System.currentTimeMillis())
        val yesterday = today - 86_400_000L
        val fmt = SimpleDateFormat("EEE, MMM d", Locale.getDefault())

        return sessions
            .groupBy { session -> startOfDay(session.startTimeMs) }
            .entries
            .sortedByDescending { it.key }
            .map { (dayMs, daySessions) ->
                val label = when (dayMs) {
                    today -> "Today"
                    yesterday -> "Yesterday"
                    else -> fmt.format(Date(dayMs))
                }
                DayGroup(
                    label = label,
                    sessions = daySessions,
                    totalMs = daySessions.sumOf { it.durationMs },
                )
            }
    }

    private fun startOfDay(ms: Long): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = ms
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }
}
