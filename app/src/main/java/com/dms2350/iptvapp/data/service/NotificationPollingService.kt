package com.dms2350.iptvapp.data.service

import android.util.Log
import com.dms2350.iptvapp.domain.model.Notification
import com.dms2350.iptvapp.domain.usecase.GetActiveNotificationsUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationPollingService @Inject constructor(
    private val getActiveNotificationsUseCase: GetActiveNotificationsUseCase
) {
    private val TAG = "NotificationPolling"
    private val scope = CoroutineScope(Dispatchers.IO)
    private var pollingJob: Job? = null

    // Intervalo de polling en milisegundos (30 segundos)
    private val POLLING_INTERVAL = 30_000L

    private val _notifications = MutableStateFlow<List<Notification>>(emptyList())
    val notifications: StateFlow<List<Notification>> = _notifications.asStateFlow()

    private val _currentNotification = MutableStateFlow<Notification?>(null)
    val currentNotification: StateFlow<Notification?> = _currentNotification.asStateFlow()

    private val shownNotificationIds = mutableSetOf<Int>()
    private var notificationDisplayJob: Job? = null

    /**
     * Inicia el polling de notificaciones
     */
    fun startPolling() {
        Log.d(TAG, "Iniciando polling de notificaciones")
        if (pollingJob?.isActive == true) {
            Log.d(TAG, "Polling ya está activo")
            return
        }

        pollingJob = scope.launch {
            while (isActive) {
                try {
                    Log.d(TAG, "Consultando notificaciones activas...")
                    val result = getActiveNotificationsUseCase()

                    result.onSuccess { notificationList ->
                        Log.d(TAG, "Se obtuvieron ${notificationList.size} notificaciones")
                        _notifications.value = notificationList
                        processNewNotifications(notificationList)
                    }.onFailure { error ->
                        Log.e(TAG, "Error al obtener notificaciones: ${error.message}")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error en polling: ${e.message}")
                }

                // Esperar antes del siguiente polling
                delay(POLLING_INTERVAL)
            }
        }
    }

    /**
     * Detiene el polling de notificaciones
     */
    fun stopPolling() {
        Log.d(TAG, "Deteniendo polling de notificaciones")
        pollingJob?.cancel()
        pollingJob = null
        notificationDisplayJob?.cancel()
        notificationDisplayJob = null
        _currentNotification.value = null
    }

    /**
     * Procesa notificaciones nuevas y las muestra
     */
    private fun processNewNotifications(notificationList: List<Notification>) {
        // Verificar si hay alguna notificación de bloqueo activa
        val hasActiveBlockedNotification = notificationList.any { it.isBlocked && it.isActive }

        // Si se está mostrando un modal de bloqueo pero ya no hay notificaciones de bloqueo activas
        // entonces limpiar el modal automáticamente
        if (_currentNotification.value?.isBlocked == true && !hasActiveBlockedNotification) {
            Log.d(TAG, "Servicio desbloqueado - limpiando modal de bloqueo")
            notificationDisplayJob?.cancel()
            _currentNotification.value = null
            // Limpiar el caché para permitir que se muestren nuevas notificaciones
            shownNotificationIds.clear()
        }

        // Filtrar notificaciones que no se han mostrado aún
        val newNotifications = notificationList.filter { notification ->
            notification.id !in shownNotificationIds && notification.isActive
        }

        if (newNotifications.isEmpty()) {
            return
        }

        // Ordenar: bloqueados primero, luego normales
        val sortedNotifications = newNotifications.sortedByDescending { it.isBlocked }

        // Si no hay una notificación mostrándose actualmente, mostrar la siguiente
        if (_currentNotification.value == null && sortedNotifications.isNotEmpty()) {
            showNotification(sortedNotifications.first())
        }
    }

    /**
     * Muestra una notificación durante el tiempo especificado
     */
    private fun showNotification(notification: Notification) {
        Log.d(TAG, "Mostrando notificación: ${notification.title}")

        notificationDisplayJob?.cancel()
        _currentNotification.value = notification

        notificationDisplayJob = scope.launch {
            // Mostrar la notificación durante el tiempo especificado
            delay(notification.displayDuration * 1000L)

            // Marcar como mostrada
            shownNotificationIds.add(notification.id)
            _currentNotification.value = null

            Log.d(TAG, "Notificación ocultada: ${notification.title}")

            // Verificar si hay más notificaciones pendientes
            val remainingNotifications = _notifications.value.filter {
                it.id !in shownNotificationIds && it.isActive
            }.sortedByDescending { it.isBlocked } // Bloqueados primero

            if (remainingNotifications.isNotEmpty()) {
                // Pequeña pausa entre notificaciones
                delay(2000L)
                showNotification(remainingNotifications.first())
            }
        }
    }

    /**
     * Descarta la notificación actual manualmente
     */
    fun dismissCurrentNotification() {
        _currentNotification.value?.let { notification ->
            shownNotificationIds.add(notification.id)
        }
        notificationDisplayJob?.cancel()
        _currentNotification.value = null
    }

    /**
     * Limpia el caché de notificaciones mostradas
     */
    fun clearShownNotifications() {
        shownNotificationIds.clear()
    }
}

