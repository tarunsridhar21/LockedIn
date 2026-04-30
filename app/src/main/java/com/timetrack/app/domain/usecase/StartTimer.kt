package com.timetrack.app.domain.usecase

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.timetrack.app.service.TimerService
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class StartTimer @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    operator fun invoke() {
        ContextCompat.startForegroundService(
            context,
            Intent(context, TimerService::class.java).setAction(TimerService.ACTION_START),
        )
    }
}
