package com.timetrack.app.data.repository

import com.timetrack.app.data.local.dao.CategoryDao
import com.timetrack.app.data.local.dao.CategoryTotal
import com.timetrack.app.data.local.dao.SessionDao
import com.timetrack.app.data.local.entity.SessionEntity
import com.timetrack.app.domain.model.Session
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionRepository @Inject constructor(
    private val sessionDao: SessionDao,
    private val categoryDao: CategoryDao,
) {
    suspend fun insert(session: Session): Long =
        sessionDao.insert(SessionEntity(
            id = session.id,
            startTimeMs = session.startTimeMs,
            endTimeMs = session.endTimeMs,
            durationMs = session.durationMs,
            categoryId = session.category?.id,
            notes = session.notes,
            createdAt = session.createdAt,
        ))

    suspend fun update(session: Session) =
        sessionDao.update(SessionEntity(
            id = session.id,
            startTimeMs = session.startTimeMs,
            endTimeMs = session.endTimeMs,
            durationMs = session.durationMs,
            categoryId = session.category?.id,
            notes = session.notes,
            createdAt = session.createdAt,
        ))

    suspend fun delete(session: Session) =
        sessionDao.delete(SessionEntity(
            id = session.id,
            startTimeMs = session.startTimeMs,
            endTimeMs = session.endTimeMs,
            durationMs = session.durationMs,
            categoryId = session.category?.id,
            notes = session.notes,
            createdAt = session.createdAt,
        ))

    suspend fun getById(id: Long): Session? {
        val entity = sessionDao.getById(id) ?: return null
        val category = entity.categoryId?.let { categoryDao.getById(it)?.toDomain() }
        return entity.toDomain(category)
    }

    fun getAll(): Flow<List<Session>> = combine(
        sessionDao.getAllPaged(),
        categoryDao.getAll(),
    ) { sessions, categories ->
        val catMap = categories.associateBy { it.id }
        sessions.map { s ->
            s.toDomain(s.categoryId?.let { catMap[it]?.toDomain() })
        }
    }

    fun getUnlabeled(): Flow<List<Session>> =
        sessionDao.getUnlabeled().map { list ->
            list.map { it.toDomain(null) }
        }

    fun getByDateRange(startMs: Long, endMs: Long): Flow<List<Session>> = combine(
        sessionDao.getByDateRange(startMs, endMs),
        categoryDao.getAll(),
    ) { sessions, categories ->
        val catMap = categories.associateBy { it.id }
        sessions.map { s ->
            s.toDomain(s.categoryId?.let { catMap[it]?.toDomain() })
        }
    }

    fun getTotalDurationByCategory(startMs: Long, endMs: Long): Flow<List<CategoryTotal>> =
        sessionDao.getTotalDurationByCategory(startMs, endMs)

    fun getTodayTotalMs(startOfDayMs: Long): Flow<Long> =
        sessionDao.getTodayTotalMs(startOfDayMs)
}

private fun SessionEntity.toDomain(category: com.timetrack.app.domain.model.Category?) = Session(
    id = id,
    startTimeMs = startTimeMs,
    endTimeMs = endTimeMs,
    durationMs = durationMs,
    category = category,
    notes = notes,
    createdAt = createdAt,
)
