package com.iptv.data.api.dto

import com.google.gson.annotations.SerializedName
import com.iptv.domain.model.Channel

data class ChannelDto(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String?,
    @SerializedName("logo_url") val logoUrl: String?,
    @SerializedName("stream_url") val streamUrl: String,
    @SerializedName("backup_stream_url") val backupStreamUrl: String?,
    @SerializedName("category_id") val categoryId: Int?,
    @SerializedName("country_id") val countryId: Int?,
    @SerializedName("language") val language: String?,
    @SerializedName("quality") val quality: String = "HD",
    @SerializedName("is_active") val isActive: Boolean = true,
    @SerializedName("is_premium") val isPremium: Boolean = false,
    @SerializedName("view_count") val viewCount: Int = 0
)

fun ChannelDto.toDomain() = Channel(
    id = id,
    name = name,
    description = description,
    logoUrl = logoUrl,
    streamUrl = streamUrl,
    backupStreamUrl = backupStreamUrl,
    categoryId = categoryId,
    countryId = countryId,
    language = language,
    quality = quality,
    isActive = isActive,
    isPremium = isPremium,
    viewCount = viewCount
)