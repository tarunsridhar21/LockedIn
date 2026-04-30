package com.timetrack.app.ui.screens.home

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.timetrack.app.R
import com.timetrack.app.domain.model.Session
import com.timetrack.app.domain.model.TimerState
import com.timetrack.app.service.TimerService

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    onNavigateToSettings: () -> Unit,
    onNavigateToLabel: (Long) -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                val sessionId = intent.getLongExtra(TimerService.EXTRA_SESSION_ID, -1L)
                if (sessionId != -1L) viewModel.onSessionSaved(sessionId)
            }
        }
        ContextCompat.registerReceiver(
            context,
            receiver,
            IntentFilter(TimerService.BROADCAST_SESSION_SAVED),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        onDispose { context.unregisterReceiver(receiver) }
    }

    LaunchedEffect(uiState.lastSavedSessionId) {
        val sessionId = uiState.lastSavedSessionId ?: return@LaunchedEffect
        val result = snackbarHostState.showSnackbar(
            message = context.getString(R.string.session_saved_message),
            actionLabel = context.getString(R.string.tap_to_label),
        )
        if (result == SnackbarResult.ActionPerformed) {
            onNavigateToLabel(sessionId)
        }
        viewModel.clearLastSaved()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.home_title)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                ),
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = stringResource(R.string.cd_settings_button),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            AnimatedTimerButton(
                timerState = uiState.timerState,
                onStart = viewModel::startTimer,
                onStop = viewModel::stopTimer,
            )

            Spacer(Modifier.height(32.dp))

            ElapsedClock(timerState = uiState.timerState)

            Spacer(Modifier.height(28.dp))

            TodayTotalCard(totalMs = uiState.todayTotalMs)

            Spacer(Modifier.height(20.dp))

            RecentSessionsRow(
                sessions = uiState.recentSessions,
                onSessionClick = { session -> onNavigateToLabel(session.id) },
            )
        }
    }
}

@Composable
private fun AnimatedTimerButton(
    timerState: TimerState,
    onStart: () -> Unit,
    onStop: () -> Unit,
) {
    val isRunning = timerState is TimerState.Running

    val buttonColor by animateColorAsState(
        targetValue = if (isRunning)
            MaterialTheme.colorScheme.tertiary
        else
            MaterialTheme.colorScheme.primary,
        animationSpec = tween(400),
        label = "button_color",
    )

    val cornerRadius by animateDpAsState(
        targetValue = if (isRunning) 55.dp else 110.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "corner_radius",
    )

    Box(
        modifier = Modifier
            .size(220.dp)
            .clip(RoundedCornerShape(cornerRadius))
            .background(buttonColor)
            .clickable { if (isRunning) onStop() else onStart() },
        contentAlignment = Alignment.Center,
    ) {
        AnimatedContent(
            targetState = isRunning,
            transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(200)) },
            label = "button_icon",
        ) { running ->
            Icon(
                imageVector = if (running) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (running)
                    stringResource(R.string.cd_stop_button)
                else
                    stringResource(R.string.cd_start_button),
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(96.dp),
            )
        }
    }
}

@Composable
private fun ElapsedClock(timerState: TimerState) {
    val elapsedMs by produceState(0L, timerState) {
        if (timerState !is TimerState.Running) {
            value = 0L
            return@produceState
        }
        while (true) {
            value = System.currentTimeMillis() - timerState.startTimeMs
            kotlinx.coroutines.delay(100)
        }
    }

    val mm = elapsedMs / 60_000
    val ss = (elapsedMs % 60_000) / 1_000
    val cs = (elapsedMs % 1_000) / 10

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "%02d:%02d".format(mm, ss),
            style = MaterialTheme.typography.displayMedium.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium,
                fontSize = 52.sp,
                letterSpacing = 2.sp,
            ),
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = ".%02d".format(cs),
            style = MaterialTheme.typography.titleMedium.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 18.sp,
                letterSpacing = 1.sp,
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun TodayTotalCard(totalMs: Long) {
    val hours = totalMs / 3_600_000
    val minutes = (totalMs % 3_600_000) / 60_000
    val label = when {
        hours > 0 -> "${hours}h ${minutes}m"
        minutes > 0 -> "${minutes}m"
        else -> "0m"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(0.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Sage left border accent
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(56.dp)
                .background(MaterialTheme.colorScheme.primary),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.today_total_label),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium,
                ),
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun RecentSessionsRow(
    sessions: List<Session>,
    onSessionClick: (Session) -> Unit,
) {
    if (sessions.isEmpty()) return

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 0.dp),
    ) {
        items(sessions) { session ->
            val colorHex = session.category?.colorHex
            val dotColor = if (colorHex != null) {
                try { Color(android.graphics.Color.parseColor(colorHex)) }
                catch (_: Exception) { MaterialTheme.colorScheme.outline }
            } else MaterialTheme.colorScheme.outline

            val durationMin = session.durationMs / 60_000
            val durationLabel = if (durationMin > 0) "${durationMin}m" else "<1m"
            val categoryName = session.category?.name ?: stringResource(R.string.unlabeled)

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(999.dp))
                    .clickable { onSessionClick(session) }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(dotColor, CircleShape),
                )
                Text(
                    text = "$categoryName · $durationLabel",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}
