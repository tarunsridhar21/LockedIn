package com.timetrack.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.timetrack.app.data.local.dao.CategoryDao
import com.timetrack.app.data.local.dao.SessionDao
import com.timetrack.app.data.local.entity.CategoryEntity
import com.timetrack.app.data.local.entity.SessionEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [SessionEntity::class, CategoryEntity::class],
    version = 2,
    exportSchema = false,
)
abstract class TimeTrackDatabase : RoomDatabase() {

    abstract fun sessionDao(): SessionDao
    abstract fun categoryDao(): CategoryDao

    class SeedCallback : Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            val presets = listOf(
                Triple("Sleeping",  "#A8B0CE", "bedtime"),
                Triple("Shower",    "#7FBFB5", "shower"),
                Triple("Studying",  "#D4A574", "school"),
                Triple("Break",     "#B5C9A8", "coffee"),
                Triple("Working",   "#8FAFD0", "work"),
                Triple("Exercise",  "#C99988", "fitness_center"),
                Triple("Reading",   "#B8A2D0", "menu_book"),
                Triple("Eating",    "#D9C384", "restaurant"),
                Triple("Commute",   "#94BFB0", "directions_car"),
            )
            CoroutineScope(Dispatchers.IO).launch {
                presets.forEachIndexed { index, (name, color, icon) ->
                    db.execSQL(
                        "INSERT OR IGNORE INTO categories (name, color_hex, icon_name, is_custom, sort_order) VALUES (?, ?, ?, 0, ?)",
                        arrayOf<Any>(name, color, icon, index),
                    )
                }
            }
        }
    }

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Desaturate preset category colors; leave custom categories untouched.
                val updates = listOf(
                    "Sleeping" to "#A8B0CE",
                    "Shower"   to "#7FBFB5",
                    "Studying" to "#D4A574",
                    "Break"    to "#B5C9A8",
                    "Working"  to "#8FAFD0",
                    "Exercise" to "#C99988",
                    "Reading"  to "#B8A2D0",
                    "Eating"   to "#D9C384",
                    "Commute"  to "#94BFB0",
                )
                updates.forEach { (name, color) ->
                    db.execSQL(
                        "UPDATE categories SET color_hex = ? WHERE name = ? AND is_custom = 0",
                        arrayOf(color, name),
                    )
                }
            }
        }
    }
}
