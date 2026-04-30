package com.timetrack.app.ui.screens.history

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.timetrack.app.R
import com.timetrack.app.domain.model.Session
import com.timetrack.app.ui.components.MetricDuration
import com.timetrack.app.ui.components.MetricStyle
import com.timetrack.app.ui.components.MonthCalendar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HistoryScreen(
    onNavigateToLabel: (Long) -> Unit,
    viewModel: HistoryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var sessionToDelete by remember { mutableStateOf<Session?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(title = {
                Text(
                    stringResource(R.string.history_title),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Medium),
                )
            })
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            // Filter chips
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    FilterChip(
                        selected = uiState.activeFilter == null,
                        onClick = { viewModel.setFilter(null) },
                        label = { Text(stringResource(R.string.filter_all)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = uiState.activeFilter == null,
                            borderColor = MaterialTheme.colorScheme.outline,
                        ),
                    )
                }
                item {
                    FilterChip(
                        selected = uiState.activeFilter == -1L,
                        onClick = { viewModel.setFilter(-1L) },
                        label = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Text(stringResource(R.string.filter_unlabeled))
                                if (uiState.unlabeledCount > 0) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .background(MaterialTheme.colorScheme.tertiary, CircleShape),
                                    )
                                }
                            }
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = uiState.activeFilter == -1L,
                            borderColor = MaterialTheme.colorScheme.outline,
                        ),
                    )
                }
                items(uiState.categories) { category ->
                    val colorHex = category.colorHex
                    FilterChip(
                        selected = uiState.activeFilter == category.id,
                        onClick = { viewModel.setFilter(category.id) },
                        label = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                if (uiState.activeFilter == category.id) {
                                    val dotColor = try {
                                        Color(android.graphics.Color.parseColor(colorHex))
                                    } catch (_: Exception) { MaterialTheme.colorScheme.primary }
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(dotColor, CircleShape),
                                    )
                                }
                                Text(category.name)
                            }
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = uiState.activeFilter == category.id,
                            borderColor = MaterialTheme.colorScheme.outline,
                        ),
                    )
                }
            }

            if (uiState.dayGroups.isEmpty()) {
                EmptyHistoryState()
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 24.dp),
                ) {
                    // Activity calendar
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            ),
                            shape = RoundedCornerShape(20.dp),
                        ) {
                            MonthCalendar(
                                yearMonth = uiState.currentMonth,
                                sessionDays = uiState.activityDays,
                                modifier = Modifier.padding(16.dp),
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                    }

                    uiState.dayGroups.forEach { group ->
                        stickyHeader {
                            DayGroupHeader(group = group)
                        }
                        items(group.sessions, key = { it.id }) { session ->
                            SessionCard(
                                session = session,
                                onClick = { onNavigateToLabel(session.id) },
                                onLongPress = { sessionToDelete = session },
                            )
                        }
                    }
                }
            }
        }
    }

    sessionToDelete?.let { session ->
        AlertDialog(
            onDismissRequest = { sessionToDelete = null },
            title = { Text(stringResource(R.string.delete_session)) },
            text = { Text(stringResource(R.string.delete_session_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteSession(session)
                    sessionToDelete = null
                }) {
                    Text(
                        stringResource(R.string.confirm),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { sessionToDelete = null }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun DayGroupHeader(group: DayGroup) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = group.label,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = "${group.sessions.size} session${if (group.sessions.size != 1) "s" else ""}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = " · ",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            MetricDuration(
                totalMs = group.totalMs,
                style = MetricStyle.Inline,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SessionCard(
    session: Session,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
) {
    val colorHex = session.category?.colorHex
    val stripeColor = if (colorHex != null) {
        try { Color(android.graphics.Color.parseColor(colorHex)) }
        catch (_: Exception) { MaterialTheme.colorScheme.tertiaryContainer }
    } else MaterialTheme.colorScheme.tertiaryContainer

    val unlabeled = session.category == null

    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1.0f,
        animationSpec = tween(durationMillis = 120),
        label = "card_scale",
    )

    val timeFmt = SimpleDateFormat("h:mm a", Locale.getDefault())
    val dateFmt = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    val startStr = timeFmt.format(Date(session.startTimeMs))
    val dateStr = dateFmt.format(Date(session.startTimeMs))
    val durationMs = session.durationMs

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .scale(scale)
            .clickable(
                onClick = onClick,
                onClickLabel = "Label session",
            ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(0.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Left color stripe
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(72.dp)
                    .clip(RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp))
                    .background(stripeColor),
            )
            Spacer(Modifier.width(12.dp))

            // Content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 14.dp),
            ) {
                if (unlabeled) {
                    Text(
                        text = stringResource(R.string.unlabeled),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontStyle = FontStyle.Italic,
                            fontWeight = FontWeight.Bold,
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Text(
                        text = session.category!!.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Spacer(Modifier.height(3.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = "$dateStr · $startStr · Duration: ",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    MetricDuration(
                        totalMs = durationMs,
                        style = MetricStyle.Inline,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // "Add label" chip for unlabeled sessions
            if (unlabeled) {
                Box(
                    modifier = Modifier
                        .padding(end = 12.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(999.dp))
                        .clickable { onClick() }
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                ) {
                    Text(
                        text = "Add label",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            } else {
                Spacer(Modifier.width(12.dp))
            }
        }
    }
}

@Composable
private fun EmptyHistoryState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // Three dashes with descending opacity
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            repeat(3) { i ->
                Box(
                    modifier = Modifier
                        .width(28.dp)
                        .height(3.dp)
                        .background(
                            MaterialTheme.colorScheme.outline.copy(alpha = 1f - i * 0.35f),
                            RoundedCornerShape(2.dp),
                        ),
                )
            }
        }
        Spacer(Modifier.height(20.dp))
        Text(
            text = stringResource(R.string.history_empty_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.history_empty_message),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

fun formatDuration(ms: Long): String {
    val totalSeconds = ms / 1_000
    val hours = totalSeconds / 3_600
    val minutes = (totalSeconds % 3_600) / 60
    val seconds = totalSeconds % 60
    return when {
        hours > 0 -> "${hours}h ${minutes}m"
        minutes > 0 -> "${minutes}m"
        else -> "${seconds}s"
    }
}
