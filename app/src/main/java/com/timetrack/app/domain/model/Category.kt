package com.timetrack.app.domain.model

data class Category(
    val id: Long = 0,
    val name: String,
    val colorHex: String,
    val iconName: String,
    val isCustom: Boolean,
    val sortOrder: Int,
)
