package com.timetrack.app.domain.model

data class Session(
    val id: Long = 0,
    val startTimeMs: Long,
    val endTimeMs: Long,
    val durationMs: Long,
    val category: Category? = null,
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
)
