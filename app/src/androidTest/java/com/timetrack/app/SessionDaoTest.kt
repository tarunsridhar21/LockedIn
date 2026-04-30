package com.timetrack.app

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.timetrack.app.data.local.TimeTrackDatabase
import com.timetrack.app.data.local.dao.SessionDao
import com.timetrack.app.data.local.entity.SessionEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SessionDaoTest {

    private lateinit var db: TimeTrackDatabase
    private lateinit var dao: SessionDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            TimeTrackDatabase::class.java,
        ).allowMainThreadQueries().build()
        dao = db.sessionDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun insertAndRetrieveSession() = runTest {
        val session = SessionEntity(startTimeMs = 1_000L, endTimeMs = 4_600_000L, durationMs = 3_600_000L)
        val id = dao.insert(session)
        val retrieved = dao.getById(id)
        assertNotNull(retrieved)
        assertEquals(3_600_000L, retrieved!!.durationMs)
    }

    @Test
    fun deleteSessionRemovesIt() = runTest {
        val session = SessionEntity(startTimeMs = 1_000L, endTimeMs = 4_600_000L, durationMs = 3_600_000L)
        val id = dao.insert(session)
        val entity = dao.getById(id)!!
        dao.delete(entity)
        assertNull(dao.getById(id))
    }

    @Test
    fun getAllPagedReturnsMostRecentFirst() = runTest {
        dao.insert(SessionEntity(startTimeMs = 1_000L, endTimeMs = 2_000L, durationMs = 1_000L))
        dao.insert(SessionEntity(startTimeMs = 5_000L, endTimeMs = 6_000L, durationMs = 1_000L))
        val sessions = dao.getAllPaged().first()
        assertEquals(2, sessions.size)
        assert(sessions[0].startTimeMs > sessions[1].startTimeMs)
    }

    @Test
    fun todayTotalReturnsCorrectSum() = runTest {
        val now = System.currentTimeMillis()
        dao.insert(SessionEntity(startTimeMs = now - 3_600_000L, endTimeMs = now - 1_800_000L, durationMs = 1_800_000L))
        dao.insert(SessionEntity(startTimeMs = now - 1_800_000L, endTimeMs = now, durationMs = 1_800_000L))
        val startOfDay = now - (now % 86_400_000L)
        val total = dao.getTodayTotalMs(startOfDay).first()
        assertEquals(3_600_000L, total)
    }
}
