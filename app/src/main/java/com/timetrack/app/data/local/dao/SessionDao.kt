package com.timetrack.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.timetrack.app.data.local.entity.SessionEntity
import kotlinx.coroutines.flow.Flow

data class CategoryTotal(
    val categoryId: Long,
    val totalMs: Long,
)

@Dao
interface SessionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: SessionEntity): Long

    @Update
    suspend fun update(session: SessionEntity)

    @Delete
    suspend fun delete(session: SessionEntity)

    @Query("SELECT * FROM sessions ORDER BY start_time_ms DESC")
    fun getAllPaged(): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions WHERE category_id IS NULL ORDER BY start_time_ms DESC")
    fun getUnlabeled(): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): SessionEntity?

    @Query("""
        SELECT * FROM sessions
        WHERE start_time_ms >= :startMs AND start_time_ms <= :endMs
        ORDER BY start_time_ms DESC
    """)
    fun getByDateRange(startMs: Long, endMs: Long): Flow<List<SessionEntity>>

    @Query("""
        SELECT category_id AS categoryId, SUM(duration_ms) AS totalMs
        FROM sessions
        WHERE start_time_ms >= :startMs AND start_time_ms <= :endMs
          AND category_id IS NOT NULL
        GROUP BY category_id
    """)
    fun getTotalDurationByCategory(startMs: Long, endMs: Long): Flow<List<CategoryTotal>>

    @Query("""
        SELECT COALESCE(SUM(duration_ms), 0)
        FROM sessions
        WHERE start_time_ms >= :startOfDayMs
    """)
    fun getTodayTotalMs(startOfDayMs: Long): Flow<Long>
}
