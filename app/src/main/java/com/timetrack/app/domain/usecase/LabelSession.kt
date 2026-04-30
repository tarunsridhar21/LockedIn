package com.timetrack.app.domain.usecase

import com.timetrack.app.data.repository.CategoryRepository
import com.timetrack.app.data.repository.SessionRepository
import com.timetrack.app.domain.model.Category
import javax.inject.Inject

class LabelSession @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val categoryRepository: CategoryRepository,
) {
    suspend operator fun invoke(sessionId: Long, category: Category, notes: String?) {
        val session = sessionRepository.getById(sessionId) ?: return
        sessionRepository.update(session.copy(category = category, notes = notes))
    }
}
