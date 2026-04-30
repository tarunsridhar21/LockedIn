package com.timetrack.app.widget

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.wrapContentSize
import androidx.glance.text.FontFamily
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.timetrack.app.data.datastore.TimerStateStore
import com.timetrack.app.widget.actions.StartTimerAction
import com.timetrack.app.widget.actions.StopTimerAction

private val BgColor     = ColorProvider(Color(0xFFF5F0EB))
private val TextPrimary = ColorProvider(Color(0xFF2D2D2B))
private val TextMuted   = ColorProvider(Color(0xFF6B6B60))
private val BtnSage     = ColorProvider(Color(0xFF7A9971))
private val BtnClay     = ColorProvider(Color(0xFFB5806A))
private val BtnText     = ColorProvider(Color.White)

class TimerGlanceWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Single

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val prefs = currentState<androidx.datastore.preferences.core.Preferences>()
            val isRunning = prefs[TimerStateStore.KEY_RUNNING] ?: false
            val startTimeMs = prefs[TimerStateStore.KEY_START_TIME_MS]
            val now = System.currentTimeMillis()

            val elapsedMs = if (isRunning && startTimeMs != null) now - startTimeMs else 0L
            val mm = elapsedMs / 60_000
            val ss = (elapsedMs % 60_000) / 1_000

            val btnColor = if (isRunning) BtnClay else BtnSage
            val btnIcon  = if (isRunning) "■" else "▶"
            val action   = if (isRunning) actionRunCallback<StopTimerAction>()
                           else actionRunCallback<StartTimerAction>()

            Row(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(BgColor)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = GlanceModifier
                        .size(44.dp)
                        .background(btnColor)
                        .clickable(action),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = btnIcon,
                        style = TextStyle(
                            color = BtnText,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                }
                Column(
                    modifier = GlanceModifier
                        .wrapContentSize()
                        .padding(start = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "%02d:%02d".format(mm, ss),
                        style = TextStyle(
                            color = TextPrimary,
                            fontSize = 18.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Medium,
                        ),
                    )
                    Text(
                        text = if (isRunning) "Running" else "Tap to start",
                        style = TextStyle(color = TextMuted, fontSize = 11.sp),
                    )
                }
            }
        }
    }
}
