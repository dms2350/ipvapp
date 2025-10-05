package com.iptv.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.iptv.domain.model.Category

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val description: String?,
    val iconUrl: String?,
    val sortOrder: Int = 0,
    val isActive: Boolean = true
)

fun CategoryEntity.toDomain() = Category(
    id = id,
    name = name,
    description = description,
    iconUrl = iconUrl,
    sortOrder = sortOrder,
    isActive = isActive
)

fun Category.toEntity() = CategoryEntity(
    id = id,
    name = name,
    description = description,
    iconUrl = iconUrl,
    sortOrder = sortOrder,
    isActive = isActive
)