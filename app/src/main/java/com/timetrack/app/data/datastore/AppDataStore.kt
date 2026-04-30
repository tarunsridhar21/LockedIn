package com.timetrack.app.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

val Context.timerDataStore: DataStore<Preferences> by preferencesDataStore(name = "timer_state")
