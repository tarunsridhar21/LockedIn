package com.timetrack.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle as JTextStyle
import java.util.Locale

@Composable
fun MonthCalendar(
    yearMonth: YearMonth,
    sessionDays: Set<LocalDate>,
    modifier: Modifier = Modifier,
    onDayClick: (LocalDate) -> Unit = {},
    onMonthChange: (YearMonth) -> Unit = {},
) {
    val today = LocalDate.now()
    val canGoForward = yearMonth.isBefore(YearMonth.now())

    val firstDay = yearMonth.atDay(1)
    val daysInMonth = yearMonth.lengthOfMonth()
    val startDayOfWeek = (firstDay.dayOfWeek.value % 7) // Mon=1..Sun=7 → Mon=1, Sun=0
    val leadingBlanks = if (startDayOfWeek == 0) 6 else startDayOfWeek - 1

    val headerFmt = DateTimeFormatter.ofPattern("MMMM yyyy")
    val monthName = yearMonth.format(headerFmt)
    val sessionDayCount = sessionDays.count { it.month == yearMonth.month && it.year == yearMonth.year }

    Column(modifier = modifier) {
        // Header row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { onMonthChange(yearMonth.minusMonths(1)) }) {
                Icon(
                    Icons.Default.ChevronLeft,
                    contentDescription = "Previous month",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = monthName,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "$sessionDayCount session ${if (sessionDayCount == 1) "day" else "days"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(
                onClick = { if (canGoForward) onMonthChange(yearMonth.plusMonths(1)) },
                enabled = canGoForward,
            ) {
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = "Next month",
                    tint = if (canGoForward)
                        MaterialTheme.colorScheme.onSurfaceVariant
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                )
            }
        }

        // Weekday header
        Spacer(Modifier.height(8.dp))
        val weekDays = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        Row(modifier = Modifier.fillMaxWidth()) {
            weekDays.forEach { day ->
                Text(
                    text = day,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(8.dp))

        // Date grid
        val totalCells = leadingBlanks + daysInMonth
        val rows = (totalCells + 6) / 7
        var cellIndex = 0

        repeat(rows) { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
            ) {
                repeat(7) { col ->
                    val dayNum = cellIndex - leadingBlanks + 1
                    cellIndex++
                    if (dayNum < 1 || dayNum > daysInMonth) {
                        Box(modifier = Modifier.weight(1f).aspectRatio(1f))
                    } else {
                        val date = yearMonth.atDay(dayNum)
                        val hasSession = sessionDays.contains(date)
                        val isToday = date == today
                        val isFuture = date.isAfter(today)

                        CalendarCell(
                            dayNum = dayNum,
                            hasSession = hasSession,
                            isToday = isToday,
                            isFuture = isFuture,
                            animationDelay = (row * 7 + col) * 40,
                            modifier = Modifier.weight(1f),
                            onClick = { onDayClick(date) },
                        )
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
        }
    }
}

@Composable
private fun CalendarCell(
    dayNum: Int,
    hasSession: Boolean,
    isToday: Boolean,
    isFuture: Boolean,
    animationDelay: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(animationDelay.toLong())
        visible = true
    }
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "cell_alpha",
    )

    val sage = MaterialTheme.colorScheme.primary
    val outline = MaterialTheme.colorScheme.outline

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .alpha(alpha)
            .padding(2.dp)
            .then(
                when {
                    hasSession -> Modifier
                        .clip(CircleShape)
                        .background(sage)
                        .then(
                            if (isToday) Modifier.border(2.dp, sage.copy(alpha = 0.6f), CircleShape)
                            else Modifier
                        )
                    isToday -> Modifier
                        .clip(CircleShape)
                        .border(2.dp, sage, CircleShape)
                    isFuture -> Modifier
                        .clip(CircleShape)
                        .border(1.dp, outline.copy(alpha = 0.4f), CircleShape)
                    else -> Modifier
                        .clip(CircleShape)
                        .border(1.dp, outline, CircleShape)
                }
            )
            .clickable(enabled = !isFuture) { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "$dayNum",
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = if (hasSession) FontWeight.Medium else FontWeight.Normal,
                fontSize = 12.sp,
            ),
            color = when {
                hasSession -> MaterialTheme.colorScheme.onPrimary
                isFuture -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                else -> MaterialTheme.colorScheme.onSurface
            },
        )
    }
}
