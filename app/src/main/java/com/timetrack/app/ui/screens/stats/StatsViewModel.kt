package com.timetrack.app.ui.screens.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.timetrack.app.data.repository.CategoryRepository
import com.timetrack.app.data.repository.SessionRepository
import com.timetrack.app.domain.model.Session
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

enum class TimeRange { TODAY, WEEK, MONTH, ALL }

data class CategoryShare(
    val name: String,
    val colorHex: String,
    val totalMs: Long,
    val percentage: Float,
)

data class TrendBar(
    val label: String,
    val valueMs: Long,
)

data class StatsUiState(
    val selectedRange: TimeRange = TimeRange.WEEK,
    val currentSessionCount: Int = 0,
    val previousSessionCount: Int = 0,
    val currentTotalMs: Long = 0L,
    val previousTotalMs: Long = 0L,
    val categoryBreakdown: List<CategoryShare> = emptyList(),
    val trendBars: List<TrendBar> = emptyList(),
    val currentMonth: YearMonth = YearMonth.now(),
    val activityDays: Set<LocalDate> = emptySet(),
    val streakDays: Int = 0,
    val isLoading: Boolean = true,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class StatsViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val categoryRepository: CategoryRepository,
) : ViewModel() {

    private val _timeRange = MutableStateFlow(TimeRange.WEEK)
    val timeRange: StateFlow<TimeRange> = _timeRange

    val uiState: StateFlow<StatsUiState> = _timeRange
        .flatMapLatest { range ->
            val (startMs, endMs) = range.toWindow()
            val (prevStartMs, prevEndMs) = range.toPreviousWindow()

            combine(
                sessionRepository.getByDateRange(startMs, endMs),
                sessionRepository.getByDateRange(prevStartMs, prevEndMs),
                categoryRepository.getAll(),
                sessionRepository.getAll(),
            ) { current, previous, categories, allSessions ->
                val catMap = categories.associateBy { it.id }

                // Category breakdown
                val catTotals = current
                    .filter { it.category != null }
                    .groupBy { it.category!! }
                    .mapValues { (_, sessions) -> sessions.sumOf { it.durationMs } }
                    .entries
                    .sortedByDescending { it.value }

                val grandTotal = catTotals.sumOf { it.value }.takeIf { it > 0L } ?: 1L
                val breakdown = catTotals.map { (cat, ms) ->
                    CategoryShare(
                        name = cat.name,
                        colorHex = cat.colorHex,
                        totalMs = ms,
                        percentage = ms.toFloat() / grandTotal,
                    )
                }

                // Trend bars
                val trendBars = buildTrendBars(range, startMs, endMs, current)

                // Activity days for calendar (current month)
                val yearMonth = YearMonth.now()
                val activityDays = allSessions
                    .map { session ->
                        Instant.ofEpochMilli(session.startTimeMs)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()
                    }
                    .filter { it.year == yearMonth.year && it.monthValue == yearMonth.monthValue }
                    .toSet()

                StatsUiState(
                    selectedRange = range,
                    currentSessionCount = current.size,
                    previousSessionCount = previous.size,
                    currentTotalMs = current.sumOf { it.durationMs },
                    previousTotalMs = previous.sumOf { it.durationMs },
                    categoryBreakdown = breakdown,
                    trendBars = trendBars,
                    currentMonth = yearMonth,
                    activityDays = activityDays,
                    streakDays = computeStreak(allSessions),
                    isLoading = false,
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = StatsUiState(),
        )

    fun setTimeRange(range: TimeRange) {
        _timeRange.value = range
    }

    private fun TimeRange.toWindow(): Pair<Long, Long> {
        val now = System.currentTimeMillis()
        val startOfToday = startOfToday()
        return when (this) {
            TimeRange.TODAY -> startOfToday to now
            TimeRange.WEEK -> (startOfToday - 6 * 86_400_000L) to now
            TimeRange.MONTH -> (startOfToday - 29 * 86_400_000L) to now
            TimeRange.ALL -> 0L to Long.MAX_VALUE
        }
    }

    private fun TimeRange.toPreviousWindow(): Pair<Long, Long> {
        val startOfToday = startOfToday()
        return when (this) {
            TimeRange.TODAY -> (startOfToday - 86_400_000L) to startOfToday
            TimeRange.WEEK -> (startOfToday - 13 * 86_400_000L) to (startOfToday - 6 * 86_400_000L)
            TimeRange.MONTH -> (startOfToday - 59 * 86_400_000L) to (startOfToday - 29 * 86_400_000L)
            TimeRange.ALL -> 0L to Long.MAX_VALUE
        }
    }

    private fun startOfToday(): Long = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private fun buildTrendBars(
        range: TimeRange,
        startMs: Long,
        endMs: Long,
        sessions: List<Session>,
    ): List<TrendBar> {
        val zone = ZoneId.systemDefault()
        return when (range) {
            TimeRange.TODAY -> {
                val byHour = sessions.groupBy { session ->
                    Instant.ofEpochMilli(session.startTimeMs).atZone(zone).hour
                }
                (0..23).map { hour ->
                    TrendBar(
                        label = if (hour % 3 == 0) "$hour" else "",
                        valueMs = byHour[hour]?.sumOf { it.durationMs } ?: 0L,
                    )
                }
            }
            TimeRange.WEEK -> {
                val byDay = sessions.groupBy { session ->
                    Instant.ofEpochMilli(session.startTimeMs).atZone(zone).toLocalDate()
                }
                val today = LocalDate.now()
                (6 downTo 0).map { daysBack ->
                    val date = today.minusDays(daysBack.toLong())
                    val label = date.dayOfWeek.getDisplayName(
                        java.time.format.TextStyle.SHORT, Locale.getDefault()
                    ).take(1)
                    TrendBar(
                        label = label,
                        valueMs = byDay[date]?.sumOf { it.durationMs } ?: 0L,
                    )
                }
            }
            TimeRange.MONTH -> {
                val byDay = sessions.groupBy { session ->
                    Instant.ofEpochMilli(session.startTimeMs).atZone(zone).dayOfMonth
                }
                val today = LocalDate.now()
                (29 downTo 0).map { daysBack ->
                    val date = today.minusDays(daysBack.toLong())
                    val sparse = date.dayOfMonth % 5 == 0 || date.dayOfMonth == 1
                    TrendBar(
                        label = if (sparse) "${date.dayOfMonth}" else "",
                        valueMs = byDay[date.dayOfMonth]?.sumOf { it.durationMs } ?: 0L,
                    )
                }
            }
            TimeRange.ALL -> {
                val byMonth = sessions.groupBy { session ->
                    Instant.ofEpochMilli(session.startTimeMs).atZone(zone).month
                }
                java.time.Month.entries.map { month ->
                    TrendBar(
                        label = month.getDisplayName(java.time.format.TextStyle.NARROW, Locale.getDefault()),
                        valueMs = byMonth[month]?.sumOf { it.durationMs } ?: 0L,
                    )
                }
            }
        }
    }

    private fun computeStreak(sessions: List<Session>): Int {
        if (sessions.isEmpty()) return 0
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now()
        val sessionDays = sessions
            .map { Instant.ofEpochMilli(it.startTimeMs).atZone(zone).toLocalDate() }
            .toSortedSet()
            .reversed()

        var streak = 0
        var expected = today
        for (day in sessionDays) {
            if (day == expected) {
                streak++
                expected = expected.minusDays(1)
            } else if (day.isBefore(expected)) {
                break
            }
        }
        return streak
    }
}
