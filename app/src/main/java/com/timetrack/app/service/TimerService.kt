package com.timetrack.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.timetrack.app.MainActivity
import com.timetrack.app.R
import androidx.datastore.preferences.core.MutablePreferences
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.appwidget.updateAll
import androidx.glance.state.PreferencesGlanceStateDefinition
import com.timetrack.app.data.datastore.TimerStateStore
import com.timetrack.app.data.repository.SessionRepository
import com.timetrack.app.domain.model.TimerState
import com.timetrack.app.widget.TimerGlanceWidget
import com.timetrack.app.widget.WidgetUpdateWorker
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class TimerService : Service() {

    companion object {
        const val ACTION_START = "com.timetrack.ACTION_START"
        const val ACTION_STOP = "com.timetrack.ACTION_STOP"
        const val ACTION_RESTORE = "com.timetrack.ACTION_RESTORE"
        const val EXTRA_SESSION_ID = "session_id"

        const val CHANNEL_ID = "timer_running"
        const val NOTIFICATION_ID = 1

        const val BROADCAST_SESSION_SAVED = "com.timetrack.SESSION_SAVED"

        // Sessions longer than 24h are considered crashed
        private const val MAX_SESSION_MS = 24L * 60 * 60 * 1000
    }

    @Inject lateinit var timerStateStore: TimerStateStore
    @Inject lateinit var sessionRepository: SessionRepository

    private val scope = CoroutineScope(Dispatchers.IO)
    private var notificationTickJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification("00:00"))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> handleStart()
            ACTION_STOP -> handleStop()
            ACTION_RESTORE -> handleRestore()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    // Android 15+ service timeout callback
    override fun onTimeout(startId: Int) {
        scope.launch {
            val startMs = timerStateStore.startTimeMs.first()
            if (startMs != null) {
                val endMs = System.currentTimeMillis()
                saveSession(startMs, endMs)
                timerStateStore.setIdleAfterSave(endMs)
                TimerStateBus.emit(TimerState.Idle)
            }
            showAutoSavedNotification()
            stopSelf()
        }
    }

    private fun handleStart() {
        val startMs = System.currentTimeMillis()
        scope.launch {
            timerStateStore.setRunning(startMs)
            TimerStateBus.emit(TimerState.Running(startMs))
            pushWidgetState {
                this[TimerStateStore.KEY_RUNNING] = true
                this[TimerStateStore.KEY_START_TIME_MS] = startMs
                remove(TimerStateStore.KEY_LAST_SAVED_AT_MS)
            }
            TimerGlanceWidget().updateAll(this@TimerService)
        }
        startNotificationTick()
    }

    private fun handleStop() {
        notificationTickJob?.cancel()
        scope.launch {
            val startMs = timerStateStore.startTimeMs.first() ?: return@launch
            val endMs = System.currentTimeMillis()

            TimerStateBus.emit(TimerState.Saving)
            pushWidgetState {
                this[TimerStateStore.KEY_RUNNING] = false
                remove(TimerStateStore.KEY_START_TIME_MS)
                this[TimerStateStore.KEY_LAST_SAVED_AT_MS] = endMs
            }
            TimerGlanceWidget().updateAll(this@TimerService)

            val sessionId = saveSession(startMs, endMs)

            timerStateStore.setIdleAfterSave(endMs)
            TimerStateBus.emit(TimerState.Idle)
            WidgetUpdateWorker.scheduleOnce(this@TimerService)
            TimerGlanceWidget().updateAll(this@TimerService)

            sendBroadcast(Intent(BROADCAST_SESSION_SAVED).putExtra(EXTRA_SESSION_ID, sessionId))
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun handleRestore() {
        scope.launch {
            val startMs = timerStateStore.startTimeMs.first()
            if (startMs != null) {
                val age = System.currentTimeMillis() - startMs
                if (age > MAX_SESSION_MS) {
                    val crashEndMs = System.currentTimeMillis()
                    saveSession(startMs, crashEndMs)
                    timerStateStore.setIdleAfterSave(crashEndMs)
                    TimerStateBus.emit(TimerState.Idle)
                    stopSelf()
                } else {
                    TimerStateBus.emit(TimerState.Running(startMs))
                    pushWidgetState {
                        this[TimerStateStore.KEY_RUNNING] = true
                        this[TimerStateStore.KEY_START_TIME_MS] = startMs
                        remove(TimerStateStore.KEY_LAST_SAVED_AT_MS)
                    }
                    TimerGlanceWidget().updateAll(this@TimerService)
                    startNotificationTick()
                }
            } else {
                TimerStateBus.emit(TimerState.Idle)
                stopSelf()
            }
        }
    }

    private suspend fun pushWidgetState(block: MutablePreferences.() -> Unit) {
        val ids = GlanceAppWidgetManager(this@TimerService)
            .getGlanceIds(TimerGlanceWidget::class.java)
        ids.forEach { id ->
            updateAppWidgetState(this@TimerService, PreferencesGlanceStateDefinition, id) { prefs ->
                prefs.toMutablePreferences().apply(block)
            }
        }
    }

    private suspend fun saveSession(startMs: Long, endMs: Long): Long {
        val duration = endMs - startMs
        return sessionRepository.insert(
            com.timetrack.app.domain.model.Session(
                startTimeMs = startMs,
                endTimeMs = endMs,
                durationMs = duration,
            )
        )
    }

    private fun startNotificationTick() {
        notificationTickJob?.cancel()
        notificationTickJob = scope.launch {
            var tick = 0
            while (true) {
                val startMs = timerStateStore.startTimeMs.first() ?: break
                val elapsed = System.currentTimeMillis() - startMs
                val mm = elapsed / 60_000
                val ss = (elapsed % 60_000) / 1_000
                updateNotification("%02d:%02d".format(mm, ss))
                tick++
                if (tick % 30 == 0) {
                    pushWidgetState {
                        this[TimerStateStore.KEY_RUNNING] = true
                        this[TimerStateStore.KEY_START_TIME_MS] = startMs
                    }
                    TimerGlanceWidget().updateAll(this@TimerService)
                }
                delay(1_000)
            }
        }
    }

    private fun updateNotification(elapsed: String) {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFICATION_ID, buildNotification(elapsed))
    }

    private fun buildNotification(elapsed: String): Notification {
        val openIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val stopIntent = PendingIntent.getService(
            this, 1,
            Intent(this, TimerService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(getString(R.string.notification_timer_running))
            .setContentText(elapsed)
            .setContentIntent(openIntent)
            .addAction(0, getString(R.string.notification_action_stop), stopIntent)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notification_channel_timer),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            setSound(null, null)
            enableVibration(false)
        }
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
            .createNotificationChannel(channel)
    }

    private fun showAutoSavedNotification() {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(getString(R.string.notification_auto_saved))
            .setAutoCancel(true)
            .build()
        nm.notify(2, notification)
    }
}
