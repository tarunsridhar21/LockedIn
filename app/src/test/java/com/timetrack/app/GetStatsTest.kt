package com.timetrack.app

import app.cash.turbine.test
import com.timetrack.app.data.local.dao.CategoryTotal
import com.timetrack.app.data.repository.CategoryRepository
import com.timetrack.app.data.repository.SessionRepository
import com.timetrack.app.domain.model.Category
import com.timetrack.app.domain.model.Session
import com.timetrack.app.domain.usecase.GetStats
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class GetStatsTest {

    private val sessionRepository = mockk<SessionRepository>()
    private val categoryRepository = mockk<CategoryRepository>()
    private lateinit var getStats: GetStats

    private val testCategory = Category(
        id = 1L,
        name = "Working",
        colorHex = "#42A5F5",
        iconName = "work",
        isCustom = false,
        sortOrder = 0,
    )

    @Before
    fun setUp() {
        getStats = GetStats(sessionRepository, categoryRepository)
        every { categoryRepository.getAll() } returns flowOf(listOf(testCategory))
    }

    @Test
    fun `empty sessions produce zero stats`() = runTest {
        every { sessionRepository.getByDateRange(any(), any()) } returns flowOf(emptyList())
        every { sessionRepository.getTotalDurationByCategory(any(), any()) } returns flowOf(emptyList())

        getStats(0L, Long.MAX_VALUE).test {
            val result = awaitItem()
            assertEquals(0L, result.totalMs)
            assertEquals(0, result.sessionCount)
            assertEquals(0L, result.longestSessionMs)
            assertEquals(null, result.topCategory)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `correct totals for a list of sessions`() = runTest {
        val now = System.currentTimeMillis()
        val sessions = listOf(
            Session(id = 1, startTimeMs = now - 7200_000, endTimeMs = now - 3600_000, durationMs = 3_600_000, category = testCategory),
            Session(id = 2, startTimeMs = now - 3600_000, endTimeMs = now, durationMs = 3_600_000, category = testCategory),
        )
        every { sessionRepository.getByDateRange(any(), any()) } returns flowOf(sessions)
        every { sessionRepository.getTotalDurationByCategory(any(), any()) } returns
            flowOf(listOf(CategoryTotal(categoryId = 1L, totalMs = 7_200_000L)))

        getStats(0L, Long.MAX_VALUE).test {
            val result = awaitItem()
            assertEquals(7_200_000L, result.totalMs)
            assertEquals(2, result.sessionCount)
            assertEquals(3_600_000L, result.longestSessionMs)
            assertEquals(testCategory, result.topCategory)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
