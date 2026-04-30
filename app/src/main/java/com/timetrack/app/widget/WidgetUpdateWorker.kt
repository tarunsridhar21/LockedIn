package com.timetrack.app.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.hilt.work.HiltWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.CoroutineWorker
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

@HiltWorker
class WidgetUpdateWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
) : CoroutineWorker(context, params) {

    companion object {
        private const val PERIODIC_WORK_NAME = "widget_update_periodic"
        private const val ONE_TIME_WORK_NAME_A = "widget_update_once_a"
        private const val ONE_TIME_WORK_NAME_B = "widget_update_once_b"

        fun schedule(context: Context, intervalSeconds: Long = 30) {
            val request = PeriodicWorkRequestBuilder<WidgetUpdateWorker>(
                intervalSeconds, TimeUnit.SECONDS,
            ).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                PERIODIC_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(PERIODIC_WORK_NAME)
        }

        // Two one-time fires after stop:
        // A at 250ms — shows "Saved" even if the action's optimistic update was missed
        // B at 1700ms — after the 1500ms saving window closes, renders final idle state
        fun scheduleOnce(context: Context) {
            val wm = WorkManager.getInstance(context)

            val requestA = OneTimeWorkRequestBuilder<WidgetUpdateWorker>()
                .setInitialDelay(250, TimeUnit.MILLISECONDS)
                .build()
            wm.enqueueUniqueWork(ONE_TIME_WORK_NAME_A, ExistingWorkPolicy.REPLACE, requestA)

            val requestB = OneTimeWorkRequestBuilder<WidgetUpdateWorker>()
                .setInitialDelay(1700, TimeUnit.MILLISECONDS)
                .build()
            wm.enqueueUniqueWork(ONE_TIME_WORK_NAME_B, ExistingWorkPolicy.REPLACE, requestB)
        }
    }

    override suspend fun doWork(): Result {
        TimerGlanceWidget().updateAll(applicationContext)
        return Result.success()
    }
}
