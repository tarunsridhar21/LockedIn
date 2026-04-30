package com.timetrack.app.domain.usecase

import com.timetrack.app.data.local.dao.CategoryTotal
import com.timetrack.app.data.repository.CategoryRepository
import com.timetrack.app.data.repository.SessionRepository
import com.timetrack.app.domain.model.Category
import com.timetrack.app.domain.model.Session
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

data class StatsResult(
    val sessions: List<Session>,
    val categoryTotals: List<Pair<Category, Long>>,
    val totalMs: Long,
    val longestSessionMs: Long,
    val topCategory: Category?,
    val sessionCount: Int,
    val streakDays: Int,
)

class GetStats @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val categoryRepository: CategoryRepository,
) {
    operator fun invoke(startMs: Long, endMs: Long): Flow<StatsResult> =
        combine(
            sessionRepository.getByDateRange(startMs, endMs),
            sessionRepository.getTotalDurationByCategory(startMs, endMs),
            categoryRepository.getAll(),
        ) { sessions, catTotals, categories ->
            val catMap = categories.associateBy { it.id }
            val enrichedTotals = catTotals.mapNotNull { ct ->
                catMap[ct.categoryId]?.let { cat -> cat to ct.totalMs }
            }.sortedByDescending { it.second }

            val streakDays = computeStreak(sessionRepository)

            StatsResult(
                sessions = sessions,
                categoryTotals = enrichedTotals,
                totalMs = sessions.sumOf { it.durationMs },
                longestSessionMs = sessions.maxOfOrNull { it.durationMs } ?: 0L,
                topCategory = enrichedTotals.firstOrNull()?.first,
                sessionCount = sessions.size,
                streakDays = streakDays,
            )
        }

    private fun computeStreak(repo: SessionRepository): Int = 0 // computed async; see StatsViewModel
}
