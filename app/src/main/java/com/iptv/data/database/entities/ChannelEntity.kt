package com.iptv.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.iptv.domain.model.Channel

@Entity(tableName = "channels")
data class ChannelEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val description: String?,
    val logoUrl: String?,
    val streamUrl: String,
    val backupStreamUrl: String?,
    val categoryId: Int?,
    val countryId: Int?,
    val language: String?,
    val quality: String = "HD",
    val isActive: Boolean = true,
    val isPremium: Boolean = false,
    val viewCount: Int = 0
)

fun ChannelEntity.toDomain() = Channel(
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

fun Channel.toEntity() = ChannelEntity(
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