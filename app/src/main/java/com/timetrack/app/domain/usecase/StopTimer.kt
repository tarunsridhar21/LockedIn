package com.timetrack.app.domain.usecase

import android.content.Context
import android.content.Intent
import com.timetrack.app.service.TimerService
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class StopTimer @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    operator fun invoke() {
        context.startService(
            Intent(context, TimerService::class.java).setAction(TimerService.ACTION_STOP),
        )
    }
}
