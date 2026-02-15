package com.dms2350.iptvapp.data.api.dto

import com.google.gson.annotations.SerializedName

data class NotificationDto(
    @SerializedName("id")
    val id: Int,

    @SerializedName("title")
    val title: String,

    @SerializedName("message")
    val message: String,

    @SerializedName("display_duration")
    val displayDuration: Int, // Duración en segundos

    @SerializedName("priority")
    val priority: String, // "high", "medium", "low"

    @SerializedName("is_active")
    val isActive: Boolean,

    @SerializedName("created_at")
    val createdAt: String?
)

data class NotificationsResponse(
    @SerializedName("is_blocked")
    val isBlocked: Boolean,

    @SerializedName("message")
    val message: String?,

    @SerializedName("notification")
    val notification: String?,

    @SerializedName("notification_duration")
    val notificationDuration: Int? // Duración en segundos
)

