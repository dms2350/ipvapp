package com.dms2350.iptvapp.data.repository

import android.content.Context
import android.provider.Settings
import com.dms2350.iptvapp.data.api.IPTVApi
import com.dms2350.iptvapp.domain.model.Notification
import com.dms2350.iptvapp.domain.repository.NotificationRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class NotificationRepositoryImpl @Inject constructor(
    private val api: IPTVApi,
    @ApplicationContext private val context: Context
) : NotificationRepository {

    override suspend fun getActiveNotifications(): Result<List<Notification>> {
        return try {
            // Obtener el device_id (ANDROID_ID)
            val deviceId = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ANDROID_ID
            )

            val response = api.getDeviceNotifications(deviceId)
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                val notifications = mutableListOf<Notification>()

                // Si el servicio está bloqueado, crear UNA notificación que combine notification y message
                if (body.isBlocked) {
                    // Combinar notification y message para el modal de bloqueo
                    val title = if (!body.notification.isNullOrBlank()) {
                        body.notification
                    } else {
                        "⚠️ Servicio Bloqueado"
                    }

                    val message = if (!body.message.isNullOrBlank()) {
                        body.message
                    } else {
                        "Su servicio ha sido bloqueado. Contacte con soporte."
                    }

                    val duration = body.notificationDuration?.takeIf { it > 0 } ?: 86400 // 24 horas por defecto para bloqueos

                    val blockedNotification = Notification(
                        id = (body.notification ?: "blocked").hashCode(),
                        title = title,
                        message = message,
                        displayDuration = duration,
                        isBlocked = true,
                        isActive = true,
                        createdAt = null
                    )
                    notifications.add(blockedNotification)
                } else {
                    // Si NO está bloqueado, manejar notification y message por separado

                    // Crear notificación principal si existe el campo "notification"
                    if (!body.notification.isNullOrBlank()) {
                        val notificationId = body.notification.hashCode()
                        val duration = body.notificationDuration?.takeIf { it > 0 } ?: 10

                        val notification = Notification(
                            id = notificationId,
                            title = "📢 Notificación",
                            message = body.notification,
                            displayDuration = duration,
                            isBlocked = false,
                            isActive = true,
                            createdAt = null
                        )
                        notifications.add(notification)
                    }

                    // Crear segunda notificación si existe el campo "message"
                    if (!body.message.isNullOrBlank()) {
                        val messageId = body.message.hashCode()
                        val duration = body.notificationDuration?.takeIf { it > 0 } ?: 10

                        val messageNotification = Notification(
                            id = messageId,
                            title = "ℹ️ Información",
                            message = body.message,
                            displayDuration = duration,
                            isBlocked = false,
                            isActive = true,
                            createdAt = null
                        )
                        notifications.add(messageNotification)
                    }
                }

                Result.success(notifications)
            } else {
                Result.failure(Exception("Error al obtener notificaciones: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

