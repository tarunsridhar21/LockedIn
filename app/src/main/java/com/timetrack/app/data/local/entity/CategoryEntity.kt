package com.timetrack.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.timetrack.app.domain.model.Category

@Entity(
    tableName = "categories",
    indices = [Index(value = ["name"], unique = true)],
)
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val name: String,

    @ColumnInfo(name = "color_hex")
    val colorHex: String,

    @ColumnInfo(name = "icon_name")
    val iconName: String,

    @ColumnInfo(name = "is_custom")
    val isCustom: Boolean,

    @ColumnInfo(name = "sort_order")
    val sortOrder: Int,
) {
    fun toDomain() = Category(
        id = id,
        name = name,
        colorHex = colorHex,
        iconName = iconName,
        isCustom = isCustom,
        sortOrder = sortOrder,
    )

    companion object {
        fun fromDomain(category: Category) = CategoryEntity(
            id = category.id,
            name = category.name,
            colorHex = category.colorHex,
            iconName = category.iconName,
            isCustom = category.isCustom,
            sortOrder = category.sortOrder,
        )
    }
}
