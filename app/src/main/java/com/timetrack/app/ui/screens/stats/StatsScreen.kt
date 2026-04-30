package com.timetrack.app.ui.screens.stats

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.columnSeries
import com.timetrack.app.R
import com.timetrack.app.ui.components.MetricCount
import com.timetrack.app.ui.components.MetricDuration
import com.timetrack.app.ui.components.MetricStyle
import com.timetrack.app.ui.components.MonthCalendar
import java.time.YearMonth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(viewModel: StatsViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.stats_title)) })
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            // Period tabs
            item {
                Spacer(Modifier.height(8.dp))
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    TimeRange.entries.forEachIndexed { index, range ->
                        SegmentedButton(
                            selected = uiState.selectedRange == range,
                            onClick = { viewModel.setTimeRange(range) },
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = TimeRange.entries.size,
                            ),
                        ) {
                            Text(
                                text = when (range) {
                                    TimeRange.TODAY -> stringResource(R.string.range_today)
                                    TimeRange.WEEK -> stringResource(R.string.range_week)
                                    TimeRange.MONTH -> stringResource(R.string.range_month)
                                    TimeRange.ALL -> stringResource(R.string.range_all)
                                },
                                style = MaterialTheme.typography.labelMedium,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(20.dp))
            }

            // Empty state
            if (uiState.currentSessionCount == 0 && !uiState.isLoading) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(200.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            stringResource(R.string.stats_empty),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
                return@LazyColumn
            }

            // Summary section
            item {
                SectionHeader(title = "Summary")
                Spacer(Modifier.height(12.dp))

                // Comparison rows
                ComparisonPair(
                    label = "Sessions",
                    prevValue = uiState.previousSessionCount.toLong(),
                    currValue = uiState.currentSessionCount.toLong(),
                    isCount = true,
                    range = uiState.selectedRange,
                )
                Spacer(Modifier.height(12.dp))
                ComparisonPair(
                    label = "Total time",
                    prevValue = uiState.previousTotalMs,
                    currValue = uiState.currentTotalMs,
                    isCount = false,
                    range = uiState.selectedRange,
                )
                Spacer(Modifier.height(8.dp))

                // "Updated live" caption
                Text(
                    text = "Updated live",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                )
                Spacer(Modifier.height(28.dp))
            }

            // Insights section
            item {
                SectionHeader(title = "Insights")
                Spacer(Modifier.height(16.dp))
            }

            // Time by category — donut
            if (uiState.categoryBreakdown.isNotEmpty()) {
                item {
                    CategoryDonutCard(breakdown = uiState.categoryBreakdown)
                    Spacer(Modifier.height(16.dp))
                }
            }

            // Trend bar chart
            if (uiState.trendBars.any { it.valueMs > 0 }) {
                item {
                    TrendCard(bars = uiState.trendBars)
                    Spacer(Modifier.height(16.dp))
                }
            }

            // Activity calendar (Month and All ranges only)
            if (uiState.selectedRange == TimeRange.MONTH || uiState.selectedRange == TimeRange.ALL) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
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
                    Spacer(Modifier.height(16.dp))
                }
            }

            // Streak pill
            item {
                StreakPill(days = uiState.streakDays)
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onBackground,
    )
}

@Composable
private fun ComparisonPair(
    label: String,
    prevValue: Long,
    currValue: Long,
    isCount: Boolean,
    range: TimeRange,
) {
    val animatedPrev by animateFloatAsState(
        targetValue = prevValue.toFloat(),
        animationSpec = tween(durationMillis = 400, easing = EaseOutCubic),
        label = "prev_anim",
    )
    val animatedCurr by animateFloatAsState(
        targetValue = currValue.toFloat(),
        animationSpec = tween(durationMillis = 400, easing = EaseOutCubic),
        label = "curr_anim",
    )

    val periodName = when (range) {
        TimeRange.TODAY -> "day"
        TimeRange.WEEK -> "week"
        TimeRange.MONTH -> "month"
        TimeRange.ALL -> "period"
    }

    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Previous period card
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
                shape = RoundedCornerShape(24.dp),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Previous $periodName",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 0.5.sp,
                    )
                    Spacer(Modifier.height(8.dp))
                    if (isCount) {
                        MetricCount(
                            count = animatedPrev.toInt(),
                            style = MetricStyle.HeroSecondary,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    } else {
                        MetricDuration(
                            totalMs = animatedPrev.toLong(),
                            style = MetricStyle.HeroSecondary,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }

            // Current period card
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                ),
                shape = RoundedCornerShape(24.dp),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = "This $periodName",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                            letterSpacing = 0.5.sp,
                        )
                        val delta = currValue - prevValue
                        if (delta != 0L && prevValue > 0L) {
                            Text(
                                text = if (delta > 0) " ▲" else " ▼",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (delta > 0)
                                    MaterialTheme.colorScheme.onPrimary
                                else
                                    MaterialTheme.colorScheme.tertiaryContainer,
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    if (isCount) {
                        MetricCount(
                            count = animatedCurr.toInt(),
                            style = MetricStyle.HeroSecondary,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    } else {
                        MetricDuration(
                            totalMs = animatedCurr.toLong(),
                            style = MetricStyle.HeroSecondary,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryDonutCard(breakdown: List<CategoryShare>) {
    val total = breakdown.sumOf { it.totalMs }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Time by Category",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
                )
                MetricDuration(
                    totalMs = total,
                    style = MetricStyle.Inline,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(16.dp))

            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                val slices = breakdown.map { share ->
                    val color = try { Color(android.graphics.Color.parseColor(share.colorHex)) }
                    catch (_: Exception) { MaterialTheme.colorScheme.primary }
                    color to share.percentage
                }
                DonutChart(
                    slices = slices,
                    modifier = Modifier.size(200.dp),
                )
                MetricDuration(
                    totalMs = total,
                    style = MetricStyle.HeroSecondary,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            Spacer(Modifier.height(16.dp))

            // Legend
            breakdown.forEach { share ->
                val color = try { Color(android.graphics.Color.parseColor(share.colorHex)) }
                catch (_: Exception) { MaterialTheme.colorScheme.primary }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(color),
                    )
                    Text(
                        text = share.name,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "${(share.percentage * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun DonutChart(
    slices: List<Pair<Color, Float>>,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val strokeWidth = 28.dp.toPx()
        val diameter = size.minDimension - strokeWidth
        val topLeft = androidx.compose.ui.geometry.Offset(
            (size.width - diameter) / 2f,
            (size.height - diameter) / 2f,
        )
        val arcSize = androidx.compose.ui.geometry.Size(diameter, diameter)

        // Empty ring if no slices
        if (slices.isEmpty() || slices.all { it.second == 0f }) {
            drawArc(
                color = Color.LightGray.copy(alpha = 0.3f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )
            return@Canvas
        }

        var startAngle = -90f
        slices.forEach { (color, proportion) ->
            val sweepAngle = proportion * 360f - 2f // small gap between slices
            if (sweepAngle > 0f) {
                drawArc(
                    color = color,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                )
            }
            startAngle += proportion * 360f
        }
    }
}

@Composable
private fun TrendCard(bars: List<TrendBar>) {
    val modelProducer = remember { CartesianChartModelProducer() }
    val sageColor = MaterialTheme.colorScheme.primary.toArgb()

    LaunchedEffect(bars) {
        modelProducer.runTransaction {
            columnSeries {
                series(bars.map { it.valueMs.toFloat() / 60_000f })
            }
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Trend",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(12.dp))
            CartesianChartHost(
                chart = rememberCartesianChart(rememberColumnCartesianLayer()),
                modelProducer = modelProducer,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp),
            )
        }
    }
}

@Composable
private fun StreakPill(days: Int) {
    var displayed by rememberSaveable { mutableStateOf(0) }
    val animatedDays by animateIntAsState(
        targetValue = displayed,
        animationSpec = tween(durationMillis = 800, easing = EaseOutCubic),
        label = "streak_anim",
    )
    LaunchedEffect(days) { displayed = days }

    val text = when (animatedDays) {
        0 -> "Start a streak today"
        1 -> "1-day streak"
        else -> "$animatedDays-day streak"
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(999.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = Icons.Default.LocalFireDepartment,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
    }
}
