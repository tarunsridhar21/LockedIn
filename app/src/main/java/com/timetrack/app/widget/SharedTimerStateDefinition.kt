package com.timetrack.app.widget

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.glance.state.GlanceStateDefinition
import com.timetrack.app.data.datastore.timerDataStore
import java.io.File

object SharedTimerStateDefinition : GlanceStateDefinition<Preferences> {
    override suspend fun getDataStore(context: Context, fileKey: String): DataStore<Preferences> =
        context.timerDataStore

    override fun getLocation(context: Context, fileKey: String): File =
        context.preferencesDataStoreFile("timer_state")
}
