package com.timetrack.app.domain.usecase

import com.timetrack.app.data.repository.SessionRepository
import kotlinx.coroutines.flow.Flow
import java.util.Calendar
import javax.inject.Inject

class GetTodayTotal @Inject constructor(
    private val sessionRepository: SessionRepository,
) {
    operator fun invoke(): Flow<Long> {
        val startOfDay = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        return sessionRepository.getTodayTotalMs(startOfDay)
    }
}
