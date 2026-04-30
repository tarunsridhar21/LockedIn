package com.timetrack.app.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import kotlinx.coroutines.flow.firstOrNull
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TimerStateStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        val KEY_RUNNING = booleanPreferencesKey("running")
        val KEY_START_TIME_MS = longPreferencesKey("start_time_ms")
        val KEY_LAST_SAVED_AT_MS = longPreferencesKey("last_saved_at_ms")
        val KEY_TODAY_TOTAL_MS = longPreferencesKey("today_total_ms")
        val KEY_WIDGET_CATEGORY_ID = longPreferencesKey("widget_category_id")
    }

    val isRunning: Flow<Boolean> = context.timerDataStore.data.map { it[KEY_RUNNING] ?: false }
    val startTimeMs: Flow<Long?> = context.timerDataStore.data.map { it[KEY_START_TIME_MS] }
    val widgetCategoryId: Flow<Long?> = context.timerDataStore.data.map { it[KEY_WIDGET_CATEGORY_ID] }

    suspend fun setWidgetCategory(categoryId: Long?) {
        context.timerDataStore.edit { prefs ->
            if (categoryId != null) prefs[KEY_WIDGET_CATEGORY_ID] = categoryId
            else prefs.remove(KEY_WIDGET_CATEGORY_ID)
        }
    }

    suspend fun setRunning(startTimeMs: Long) {
        context.timerDataStore.edit { prefs ->
            prefs[KEY_RUNNING] = true
            prefs[KEY_START_TIME_MS] = startTimeMs
            prefs.remove(KEY_LAST_SAVED_AT_MS)
        }
    }

    suspend fun setIdleAfterSave(savedAtMs: Long) {
        context.timerDataStore.edit { prefs ->
            prefs[KEY_RUNNING] = false
            prefs.remove(KEY_START_TIME_MS)
            prefs[KEY_LAST_SAVED_AT_MS] = savedAtMs
        }
    }

    suspend fun getSnapshot(): Preferences = context.timerDataStore.data
        .map { it }
        .let { flow ->
            var result: Preferences? = null
            flow.collect { result = it }
            @Suppress("UNCHECKED_CAST")
            result!!
        }

    val dataStore: DataStore<Preferences> get() = context.timerDataStore
}
