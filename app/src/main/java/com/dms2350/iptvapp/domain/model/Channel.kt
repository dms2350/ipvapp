package com.dms2350.iptvapp.domain.model

data class Channel(
    val id: Int,
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
