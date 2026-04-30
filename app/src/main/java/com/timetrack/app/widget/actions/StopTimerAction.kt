package com.timetrack.app.widget.actions

import android.content.Context
import android.content.Intent
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.appwidget.updateAll
import androidx.glance.state.PreferencesGlanceStateDefinition
import com.timetrack.app.data.datastore.TimerStateStore
import com.timetrack.app.service.TimerService
import com.timetrack.app.widget.TimerGlanceWidget

class StopTimerAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { prefs ->
            prefs.toMutablePreferences().apply {
                this[TimerStateStore.KEY_RUNNING] = false
                remove(TimerStateStore.KEY_START_TIME_MS)
            }
        }
        TimerGlanceWidget().updateAll(context)

        context.startService(
            Intent(context, TimerService::class.java).setAction(TimerService.ACTION_STOP),
        )
    }
}
