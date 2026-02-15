package com.dms2350.iptvapp.domain.repository

import com.dms2350.iptvapp.domain.model.Notification

interface NotificationRepository {
    suspend fun getActiveNotifications(): Result<List<Notification>>
}

