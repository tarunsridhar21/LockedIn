package com.timetrack.app.widget

import androidx.glance.appwidget.GlanceAppWidgetReceiver

class TimerWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = TimerGlanceWidget()
}
