package com.dms2350.iptvapp.domain.usecase

import com.dms2350.iptvapp.domain.model.Notification
import com.dms2350.iptvapp.domain.repository.NotificationRepository
import javax.inject.Inject

class GetActiveNotificationsUseCase @Inject constructor(
    private val repository: NotificationRepository
) {
    suspend operator fun invoke(): Result<List<Notification>> {
        return repository.getActiveNotifications()
    }
}

