package com.dms2350.iptvapp.presentation.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.dms2350.iptvapp.data.service.NotificationPollingService
import com.dms2350.iptvapp.domain.model.Notification
import com.dms2350.iptvapp.presentation.ui.player.VLCPlayerManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class NotificationViewModel @Inject constructor(
    private val notificationPollingService: NotificationPollingService,
    private val vlcPlayerManager: VLCPlayerManager
) : ViewModel() {

    val currentNotification: StateFlow<Notification?> =
        notificationPollingService.currentNotification

    val notifications: StateFlow<List<Notification>> =
        notificationPollingService.notifications

    private val _isServiceBlocked = MutableStateFlow(false)
    val isServiceBlocked: StateFlow<Boolean> = _isServiceBlocked.asStateFlow()

    private var navController: NavController? = null

    init {
        // Observar cambios en las notificaciones para pausar/reanudar reproducción
        viewModelScope.launch {
            currentNotification.collect { notification ->
                handleNotificationChange(notification)
            }
        }
    }

    fun setNavController(controller: NavController) {
        navController = controller
    }

    private fun handleNotificationChange(notification: Notification?) {
        val wasBlocked = _isServiceBlocked.value
        val isNowBlocked = notification?.isBlocked == true

        _isServiceBlocked.value = isNowBlocked

        if (isNowBlocked && !wasBlocked) {
            // Servicio recién bloqueado - destruir reproductor y navegar a inicio
            Timber.d("IPTV: Servicio bloqueado - destruyendo reproductor y navegando a inicio")
            try {
                // Liberar completamente el reproductor (destruye MediaPlayer y LibVLC)
                vlcPlayerManager.release()

                // Navegar a la pantalla de canales (inicio)
                navController?.navigate("channels") {
                    popUpTo("channels") { inclusive = true }
                    launchSingleTop = true
                }
            } catch (e: Exception) {
                Timber.e("IPTV: Error al destruir reproductor: ${e.message}")
            }
        } else if (!isNowBlocked && wasBlocked) {
            // Servicio desbloqueado
            Timber.d("IPTV: Servicio desbloqueado - usuario puede usar la app normalmente")
        }
    }

    fun dismissNotification() {
        notificationPollingService.dismissCurrentNotification()
    }
}

