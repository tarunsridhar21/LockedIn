package com.timetrack.app.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class MetricStyle { Hero, HeroSecondary, Inline }

@Composable
fun MetricDuration(
    totalMs: Long,
    style: MetricStyle = MetricStyle.Hero,
    color: Color = LocalContentColor.current,
) {
    val hours = totalMs / 3_600_000L
    val minutes = (totalMs % 3_600_000L) / 60_000L

    val (numeralSp, unitSp) = when (style) {
        MetricStyle.Hero -> 36.sp to 20.sp
        MetricStyle.HeroSecondary -> 28.sp to 16.sp
        MetricStyle.Inline -> 14.sp to 14.sp
    }
    val numeralWeight = FontWeight.Bold
    val unitWeight = if (style == MetricStyle.Inline) FontWeight.Normal else FontWeight.Medium

    val tabular = TextStyle(fontFeatureSettings = "tnum")

    Row(verticalAlignment = Alignment.Bottom) {
        if (hours > 0L) {
            Text(
                text = "$hours",
                style = tabular.copy(
                    fontSize = numeralSp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = numeralWeight,
                    color = color,
                ),
            )
            Text(
                text = "H",
                style = TextStyle(
                    fontSize = unitSp,
                    fontWeight = unitWeight,
                    color = color,
                ),
            )
            Spacer(Modifier.width(4.dp))
        }
        Text(
            text = "$minutes",
            style = tabular.copy(
                fontSize = numeralSp,
                fontFamily = FontFamily.Monospace,
                fontWeight = numeralWeight,
                color = color,
            ),
        )
        Text(
            text = "M",
            style = TextStyle(
                fontSize = unitSp,
                fontWeight = unitWeight,
                color = color,
            ),
        )
    }
}

@Composable
fun MetricCount(
    count: Int,
    style: MetricStyle = MetricStyle.Hero,
    unit: String = "",
    color: Color = LocalContentColor.current,
) {
    val (numeralSp, unitSp) = when (style) {
        MetricStyle.Hero -> 36.sp to 20.sp
        MetricStyle.HeroSecondary -> 28.sp to 16.sp
        MetricStyle.Inline -> 14.sp to 14.sp
    }
    val numeralWeight = FontWeight.Bold
    val unitWeight = if (style == MetricStyle.Inline) FontWeight.Normal else FontWeight.Medium

    Row(verticalAlignment = Alignment.Bottom) {
        Text(
            text = "$count",
            style = TextStyle(
                fontSize = numeralSp,
                fontFamily = FontFamily.Monospace,
                fontWeight = numeralWeight,
                fontFeatureSettings = "tnum",
                color = color,
            ),
        )
        if (unit.isNotEmpty()) {
            Spacer(Modifier.width(2.dp))
            Text(
                text = unit,
                style = TextStyle(
                    fontSize = unitSp,
                    fontWeight = unitWeight,
                    color = color,
                ),
            )
        }
    }
}
