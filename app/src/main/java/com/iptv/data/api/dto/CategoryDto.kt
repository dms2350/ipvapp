package com.iptv.data.api.dto

import com.google.gson.annotations.SerializedName
import com.iptv.domain.model.Category

data class CategoryDto(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String?,
    @SerializedName("icon_url") val iconUrl: String?,
    @SerializedName("sort_order") val sortOrder: Int = 0,
    @SerializedName("is_active") val isActive: Boolean = true
)

fun CategoryDto.toDomain() = Category(
    id = id,
    name = name,
    description = description,
    iconUrl = iconUrl,
    sortOrder = sortOrder,
    isActive = isActive
)