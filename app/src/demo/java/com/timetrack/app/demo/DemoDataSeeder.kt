package com.timetrack.app.demo

import android.content.Context
import com.timetrack.app.data.local.dao.CategoryDao
import com.timetrack.app.data.local.dao.SessionDao
import com.timetrack.app.data.local.entity.CategoryEntity
import com.timetrack.app.data.local.entity.SessionEntity
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import java.util.Calendar
import kotlin.random.Random

@EntryPoint
@InstallIn(SingletonComponent::class)
interface DemoSeederEntryPoint {
    fun sessionDao(): SessionDao
    fun categoryDao(): CategoryDao
}

class DemoDataSeeder(private val context: Context) {

    private val rng = Random(42)

    suspend fun seed() {
        val prefs = context.getSharedPreferences("demo_prefs", Context.MODE_PRIVATE)
        if (prefs.getBoolean("seeded", false)) return

        val ep = EntryPointAccessors.fromApplication(
            context.applicationContext,
            DemoSeederEntryPoint::class.java,
        )
        val sessionDao = ep.sessionDao()
        val categoryDao = ep.categoryDao()

        // SeedCallback is async; wait for default categories to appear
        var attempts = 0
        var categories = categoryDao.getAll().first()
        while (categories.isEmpty() && attempts++ < 20) {
            delay(300)
            categories = categoryDao.getAll().first()
        }
        if (categories.isEmpty()) return

        // Insert Masters-student custom categories
        categoryDao.insert(CategoryEntity(name = "Research",     colorHex = "#9C8EC9", iconName = "science",  isCustom = true, sortOrder = 9))
        categoryDao.insert(CategoryEntity(name = "Lectures",     colorHex = "#6B9DC2", iconName = "school",   isCustom = true, sortOrder = 10))
        categoryDao.insert(CategoryEntity(name = "Office Hours", colorHex = "#C2956B", iconName = "people",   isCustom = true, sortOrder = 11))

        categories = categoryDao.getAll().first()
        val catId: (String) -> Long? = { name -> categories.find { it.name == name }?.id }

        val midnight = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0);      set(Calendar.MILLISECOND, 0)
        }

        for (daysAgo in 45 downTo 0) {
            val dayStart = (midnight.clone() as Calendar).apply {
                add(Calendar.DAY_OF_YEAR, -daysAgo)
            }
            val dow = dayStart.get(Calendar.DAY_OF_WEEK)
            val isWeekend = dow == Calendar.SATURDAY || dow == Calendar.SUNDAY
            buildDay(dayStart.timeInMillis, isWeekend, daysAgo == 0, catId)
                .forEach { sessionDao.insert(it) }
        }

        prefs.edit().putBoolean("seeded", true).apply()
    }

    private fun buildDay(
        dayMs: Long,
        isWeekend: Boolean,
        isToday: Boolean,
        catId: (String) -> Long?,
    ): List<SessionEntity> {
        val out = mutableListOf<SessionEntity>()
        val h = 3_600_000L
        val m = 60_000L

        fun session(offsetMs: Long, durationMs: Long, cat: String, notes: String? = null) {
            out += SessionEntity(
                startTimeMs = dayMs + offsetMs,
                endTimeMs   = dayMs + offsetMs + durationMs,
                durationMs  = durationMs,
                categoryId  = catId(cat),
                notes       = notes,
            )
        }

        // Sleep — starts at midnight, lasts until wake-up
        val sleepDur = if (isWeekend) rng.nextLong(7 * h, 9 * h) else rng.nextLong(6 * h + 30 * m, 8 * h)
        session(0, sleepDur, "Sleeping")
        var t = sleepDur

        // Morning shower
        session(t, rng.nextLong(8 * m, 15 * m), "Shower")
        t += rng.nextLong(10 * m, 20 * m)

        // Breakfast
        session(t, rng.nextLong(15 * m, 25 * m), "Eating", pickOpt(breakfastNotes, 0.4f))
        t += rng.nextLong(20 * m, 35 * m)

        if (isToday) {
            // Only log what's happened so far this morning — leave timer fresh
            if (!isWeekend) {
                session(t, rng.nextLong(25 * m, 38 * m), "Commute", "bus to campus")
                t += rng.nextLong(30 * m, 48 * m)
                session(t, rng.nextLong(50 * m, 75 * m), "Lectures", pick(lectureNotes))
                t += rng.nextLong(55 * m, 85 * m)
                session(t, rng.nextLong(40 * m, 80 * m), "Studying", pick(studyNotes))
            } else {
                session(t, rng.nextLong(40 * m, 70 * m), "Reading", pick(readingNotes))
            }
            return out
        }

        if (!isWeekend) {
            // Commute to campus
            session(t, rng.nextLong(22 * m, 38 * m), "Commute", "bus to campus")
            t += rng.nextLong(28 * m, 48 * m)

            // Morning lecture (70% of weekdays)
            if (rng.nextFloat() < 0.7f) {
                session(t, rng.nextLong(50 * m, 80 * m), "Lectures", pick(lectureNotes))
                t += rng.nextLong(55 * m, 90 * m)
            }

            // Morning study block
            session(t, rng.nextLong(45 * m, 105 * m), "Studying", pick(studyNotes))
            t += rng.nextLong(50 * m, 120 * m)

            // Lunch
            session(t, rng.nextLong(25 * m, 45 * m), "Eating", pickOpt(lunchNotes, 0.5f))
            t += rng.nextLong(30 * m, 55 * m)

            // Afternoon: lecture, office hours, or free slot
            when {
                rng.nextFloat() < 0.35f -> {
                    session(t, rng.nextLong(50 * m, 75 * m), "Lectures", pick(lectureNotes))
                    t += rng.nextLong(55 * m, 85 * m)
                }
                rng.nextFloat() < 0.25f -> {
                    session(t, rng.nextLong(30 * m, 55 * m), "Office Hours", pick(officeNotes))
                    t += rng.nextLong(35 * m, 65 * m)
                }
                else -> t += rng.nextLong(10 * m, 30 * m)
            }

            // Research block (80% of weekdays)
            if (rng.nextFloat() < 0.8f) {
                session(t, rng.nextLong(90 * m, 190 * m), "Research", pick(researchNotes))
                t += rng.nextLong(100 * m, 210 * m)
            }

            // Break
            if (rng.nextFloat() < 0.7f) {
                session(t, rng.nextLong(10 * m, 22 * m), "Break")
                t += rng.nextLong(12 * m, 25 * m)
            }

            // TA / working (50% of weekdays)
            if (rng.nextFloat() < 0.5f) {
                session(t, rng.nextLong(90 * m, 180 * m), "Working", pick(workNotes))
                t += rng.nextLong(100 * m, 200 * m)
            }

            // Commute home
            session(t, rng.nextLong(22 * m, 38 * m), "Commute", "walk / bus home")
            t += rng.nextLong(28 * m, 48 * m)

        } else {
            // Weekend: brunch, lighter schedule
            t += rng.nextLong(30 * m, 60 * m)
            session(t, rng.nextLong(25 * m, 45 * m), "Eating", pickOpt(lunchNotes, 0.3f))
            t += rng.nextLong(30 * m, 60 * m)

            if (rng.nextFloat() < 0.65f) {
                session(t, rng.nextLong(45 * m, 110 * m), "Studying", pick(studyNotes))
                t += rng.nextLong(50 * m, 125 * m)
            }
            if (rng.nextFloat() < 0.75f) {
                session(t, rng.nextLong(60 * m, 120 * m), "Research", pick(researchNotes))
                t += rng.nextLong(70 * m, 135 * m)
            }
            // Weekend break
            if (rng.nextFloat() < 0.5f) {
                session(t, rng.nextLong(15 * m, 35 * m), "Break")
                t += rng.nextLong(20 * m, 40 * m)
            }
        }

        // Exercise (60% weekday, 55% weekend)
        if (rng.nextFloat() < if (isWeekend) 0.55f else 0.60f) {
            session(t, rng.nextLong(35 * m, 65 * m), "Exercise", pick(exerciseNotes))
            t += rng.nextLong(40 * m, 75 * m)
            // Post-exercise shower (45%)
            if (rng.nextFloat() < 0.45f) {
                session(t, rng.nextLong(8 * m, 14 * m), "Shower")
                t += rng.nextLong(10 * m, 18 * m)
            }
        }

        // Dinner — at least 11h after waking
        t = t.coerceAtLeast(sleepDur + 11 * h)
        session(t, rng.nextLong(30 * m, 55 * m), "Eating", pickOpt(dinnerNotes, 0.5f))
        t += rng.nextLong(35 * m, 65 * m)

        // Evening reading
        if (rng.nextFloat() < 0.8f) {
            session(t, rng.nextLong(25 * m, 65 * m), "Reading", pick(readingNotes))
            t += rng.nextLong(30 * m, 75 * m)
        }

        // Evening study
        if (rng.nextFloat() < if (isWeekend) 0.45f else 0.75f) {
            session(t, rng.nextLong(45 * m, 120 * m), "Studying", pick(studyNotes))
            t += rng.nextLong(50 * m, 130 * m)
        }

        // Occasional late-night break
        if (rng.nextFloat() < 0.45f) {
            session(t, rng.nextLong(8 * m, 20 * m), "Break")
        }

        return out
    }

    // ── Note pools ────────────────────────────────────────────────────────────

    private val lectureNotes = listOf(
        "Distributed Systems", "ML Theory", "Advanced Algorithms",
        "Computer Vision", "NLP Seminar", "Research Methods",
        "Reinforcement Learning", "Cloud Computing", "Database Systems",
        "Bayesian Statistics",
    )

    private val studyNotes = listOf(
        "problem set 3", "exam prep", "reviewing lecture notes",
        "past papers", "assignment 2", "midterm review",
        "final project planning", "group study session",
        null, null, null,
    )

    private val researchNotes = listOf(
        "thesis ch2 draft", "running experiments", "literature review",
        "data analysis", "model training", "writing related work",
        "debugging pipeline", "advisor feedback revisions",
        "reading survey papers", "prototyping new approach",
        "thesis ch3 outline",
    )

    private val readingNotes = listOf(
        "NeurIPS paper", "ICLR submission", "textbook ch7",
        "The Remains of the Day", "survey paper", null,
        "documentation", null, "blog post", "ICML paper",
    )

    private val exerciseNotes = listOf(
        "5k run", "gym — push day", "gym — pull day",
        "gym — legs", "yoga", "cycling", "home workout", null,
    )

    private val workNotes = listOf(
        "grading assignments", "TA office hours",
        "marking submissions", "preparing tutorial slides", "lab session",
    )

    private val officeNotes = listOf(
        "advisor 1:1", "progress update",
        "thesis outline review", "feedback on ch2 draft", "career chat",
    )

    private val breakfastNotes = listOf("oats at home", "toast", "dining hall", null, null)
    private val lunchNotes     = listOf("dining hall", "meal prepped", "with lab mates", null, null)
    private val dinnerNotes    = listOf("cooked pasta", "dining hall", "takeaway", "with housemates", "leftovers", null)

    private fun pick(pool: List<String?>): String? = pool[rng.nextInt(pool.size)]
    private fun pickOpt(pool: List<String?>, p: Float): String? =
        if (rng.nextFloat() < p) pick(pool) else null
}
