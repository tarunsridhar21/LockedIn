package com.timetrack.app.widget.actions

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.appwidget.updateAll
import androidx.glance.state.PreferencesGlanceStateDefinition
import com.timetrack.app.data.datastore.TimerStateStore
import com.timetrack.app.service.TimerService
import com.timetrack.app.widget.TimerGlanceWidget

class StartTimerAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val startMs = System.currentTimeMillis()

        updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { prefs ->
            prefs.toMutablePreferences().apply {
                this[TimerStateStore.KEY_RUNNING] = true
                this[TimerStateStore.KEY_START_TIME_MS] = startMs
                remove(TimerStateStore.KEY_LAST_SAVED_AT_MS)
            }
        }
        TimerGlanceWidget().updateAll(context)

        ContextCompat.startForegroundService(
            context,
            Intent(context, TimerService::class.java).setAction(TimerService.ACTION_START),
        )
    }
}
