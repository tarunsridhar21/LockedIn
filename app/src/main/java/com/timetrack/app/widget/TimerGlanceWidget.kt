package com.timetrack.app.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
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
import androidx.glance.layout.wrapContentSize
import androidx.glance.text.FontFamily
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.timetrack.app.data.datastore.TimerStateStore
import com.timetrack.app.widget.actions.StartTimerAction
import com.timetrack.app.widget.actions.StopTimerAction

private val TimerIdleSage = Color(0xFF7A9971)
private val TimerRunningClay = Color(0xFFB5806A)
private val TimerSavingGold = Color(0xFFD4A574)

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

    GlanceTheme {
        when {
            size.height >= 160.dp && size.width >= 280.dp -> LargeWidget(isRunning, isSaving, timeText)
            size.height >= 160.dp -> MediumWidget(isRunning, isSaving, timeText)
            else -> SmallWidget(isRunning, isSaving, timeText)
        }
    }
}

@Composable
private fun SmallWidget(isRunning: Boolean, isSaving: Boolean, timeText: String) {
    Row(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.surface)
            .cornerRadius(28)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TimerButton(isRunning, isSaving, size = 40.dp)
        Spacer(GlanceModifier.width(8.dp))
        Text(
            text = if (isSaving) "Saved" else timeText,
            style = TextStyle(
                color = GlanceTheme.colors.onSurface,
                fontSize = 16.sp,
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
            .background(GlanceTheme.colors.surface)
            .cornerRadius(28)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TimerButton(isRunning, isSaving, size = 72.dp)
        Spacer(GlanceModifier.padding(top = 8.dp))
        Text(
            text = if (isSaving) "Saved" else timeText,
            style = TextStyle(
                color = GlanceTheme.colors.onSurface,
                fontSize = 20.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium,
            ),
        )
        Spacer(GlanceModifier.padding(top = 4.dp))
        Text(
            text = if (isRunning) "Running" else if (isSaving) "Saving…" else "Tap to start",
            style = TextStyle(
                color = GlanceTheme.colors.onSurfaceVariant,
                fontSize = 12.sp,
            ),
        )
    }
}

@Composable
private fun LargeWidget(isRunning: Boolean, isSaving: Boolean, timeText: String) {
    Row(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.surface)
            .cornerRadius(28)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = GlanceModifier.wrapContentSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TimerButton(isRunning, isSaving, size = 72.dp)
            Spacer(GlanceModifier.padding(top = 8.dp))
            Text(
                text = if (isSaving) "Saved" else timeText,
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontSize = 20.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium,
                ),
            )
            Spacer(GlanceModifier.padding(top = 4.dp))
            Text(
                text = if (isRunning) "Running" else if (isSaving) "Saving…" else "Tap to start",
                style = TextStyle(
                    color = GlanceTheme.colors.onSurfaceVariant,
                    fontSize = 12.sp,
                ),
            )
        }
    }
}

@Composable
private fun TimerButton(
    isRunning: Boolean,
    isSaving: Boolean,
    size: androidx.compose.ui.unit.Dp,
) {
    val bgColor = when {
        isSaving -> ColorProvider(TimerSavingGold)
        isRunning -> ColorProvider(TimerRunningClay)
        else -> ColorProvider(TimerIdleSage)
    }
    val action = if (isRunning || isSaving) {
        actionRunCallback<StopTimerAction>()
    } else {
        actionRunCallback<StartTimerAction>()
    }
    val iconText = when {
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
            text = iconText,
            style = TextStyle(
                color = ColorProvider(Color.White),
                fontSize = (size.value * 0.36f).sp,
                fontWeight = FontWeight.Bold,
            ),
        )
    }
}
