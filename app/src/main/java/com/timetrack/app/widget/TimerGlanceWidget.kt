package com.timetrack.app.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontFamily
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.timetrack.app.data.datastore.TimerStateStore
import com.timetrack.app.widget.actions.StartTimerAction
import com.timetrack.app.widget.actions.StopTimerAction

// Static palette — no GlanceTheme/dynamic color needed
private val BgColor       = ColorProvider(Color(0xFFF5F0EB))
private val TextColor     = ColorProvider(Color(0xFF2D2D2B))
private val SubtextColor  = ColorProvider(Color(0xFF6B6B60))
private val SageGreen     = ColorProvider(Color(0xFF7A9971))
private val ClayRed       = ColorProvider(Color(0xFFB5806A))
private val SaveGold      = ColorProvider(Color(0xFFD4A574))
private val White         = ColorProvider(Color.White)

class TimerGlanceWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            WidgetContent()
        }
    }
}

@Composable
private fun WidgetContent() {
    val prefs = currentState<androidx.datastore.preferences.core.Preferences>()
    val isRunning = prefs[TimerStateStore.KEY_RUNNING] ?: false
    val startTimeMs = prefs[TimerStateStore.KEY_START_TIME_MS]
    val lastSavedAt = prefs[TimerStateStore.KEY_LAST_SAVED_AT_MS] ?: 0L
    val now = System.currentTimeMillis()
    val isSaving = !isRunning && lastSavedAt > 0L && (now - lastSavedAt) in 0..1500L

    val elapsedMs = if (isRunning && startTimeMs != null) now - startTimeMs else 0L
    val mm = elapsedMs / 60_000
    val ss = (elapsedMs % 60_000) / 1_000
    val timeText = "%02d:%02d".format(mm, ss)

    val size = LocalSize.current

    when {
        size.height >= 120.dp && size.width >= 200.dp -> LargeWidget(isRunning, isSaving, timeText)
        size.height >= 100.dp -> MediumWidget(isRunning, isSaving, timeText)
        else -> SmallWidget(isRunning, isSaving, timeText)
    }
}

@Composable
private fun SmallWidget(isRunning: Boolean, isSaving: Boolean, timeText: String) {
    Row(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(BgColor)
            .cornerRadius(24)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TimerButton(isRunning, isSaving, size = 36.dp)
        Spacer(GlanceModifier.width(8.dp))
        Text(
            text = if (isSaving) "Saved" else timeText,
            style = TextStyle(
                color = TextColor,
                fontSize = 15.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium,
            ),
        )
    }
}

@Composable
private fun MediumWidget(isRunning: Boolean, isSaving: Boolean, timeText: String) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(BgColor)
            .cornerRadius(24)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TimerButton(isRunning, isSaving, size = 64.dp)
        Spacer(GlanceModifier.padding(top = 8.dp))
        Text(
            text = if (isSaving) "Saved" else timeText,
            style = TextStyle(
                color = TextColor,
                fontSize = 18.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium,
            ),
        )
        Spacer(GlanceModifier.padding(top = 2.dp))
        Text(
            text = if (isRunning) "Running" else if (isSaving) "Saving…" else "Tap to start",
            style = TextStyle(color = SubtextColor, fontSize = 11.sp),
        )
    }
}

@Composable
private fun LargeWidget(isRunning: Boolean, isSaving: Boolean, timeText: String) {
    Row(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(BgColor)
            .cornerRadius(24)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TimerButton(isRunning, isSaving, size = 64.dp)
        Spacer(GlanceModifier.width(16.dp))
        Column(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = if (isSaving) "Saved" else timeText,
                style = TextStyle(
                    color = TextColor,
                    fontSize = 22.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium,
                ),
            )
            Spacer(GlanceModifier.padding(top = 2.dp))
            Text(
                text = if (isRunning) "Running" else if (isSaving) "Saving…" else "Tap to start",
                style = TextStyle(color = SubtextColor, fontSize = 11.sp),
            )
        }
    }
}

@Composable
private fun TimerButton(isRunning: Boolean, isSaving: Boolean, size: androidx.compose.ui.unit.Dp) {
    val bgColor = when {
        isSaving -> SaveGold
        isRunning -> ClayRed
        else -> SageGreen
    }
    val action = if (isRunning || isSaving) actionRunCallback<StopTimerAction>()
                 else actionRunCallback<StartTimerAction>()
    val icon = when {
        isSaving -> "✓"
        isRunning -> "■"
        else -> "▶"
    }
    Box(
        modifier = GlanceModifier
            .size(size)
            .background(bgColor)
            .cornerRadius(999)
            .clickable(action),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = icon,
            style = TextStyle(
                color = White,
                fontSize = (size.value * 0.36f).sp,
                fontWeight = FontWeight.Bold,
            ),
        )
    }
}
