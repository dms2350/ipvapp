package com.dms2350.iptvapp.presentation

import android.content.Context
import android.media.AudioManager
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.dms2350.iptvapp.data.local.UserPreferences
import com.dms2350.iptvapp.data.service.DeviceHeartbeatService
import com.dms2350.iptvapp.data.service.NotificationPollingService
import com.dms2350.iptvapp.presentation.ui.player.VLCPlayerManager
import com.dms2350.iptvapp.presentation.theme.IPTVTheme
import com.dms2350.iptvapp.presentation.ui.MainScreen
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var vlcPlayerManager: VLCPlayerManager

    @Inject
    lateinit var heartbeatService: DeviceHeartbeatService

    @Inject
    lateinit var notificationPollingService: NotificationPollingService

    @Inject
    lateinit var userPreferences: UserPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Detectar dispositivos con restricciones
        val isRestrictedDevice = android.os.Build.MANUFACTURER.contains("Rockchip", ignoreCase = true) ||
                                android.os.Build.MANUFACTURER.contains("Allwinner", ignoreCase = true) ||
                                android.os.Build.MANUFACTURER.contains("Amlogic", ignoreCase = true) ||
                                android.os.Build.MANUFACTURER.contains("Premier", ignoreCase = true) ||
                                android.os.Build.MODEL.contains("Premier", ignoreCase = true) ||
                                android.os.Build.MODEL.contains("TV Box", ignoreCase = true)
        
        if (isRestrictedDevice) {
            // Para dispositivos con restricciones, usar stream de llamada
            volumeControlStream = AudioManager.STREAM_VOICE_CALL
            setVolumeControlStream(AudioManager.STREAM_VOICE_CALL)
            
            // Configuración adicional para dispositivos con restricciones
            try {
                val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
                audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
                audioManager.isSpeakerphoneOn = true
                Timber.d("IPTV: MainActivity configurado para streams HTTP")
            } catch (e: Exception) {
                Timber.d("IPTV: Error configurando MainActivity para dispositivo con restricciones: ${e.message}")
            }
        }
        
        // Configurar pantalla completa para TV y móvil
        setupFullScreenForTV()
        enableEdgeToEdge()
        
        setContent {
            IPTVTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(userPreferences = userPreferences)
                }
            }
        }

        // Iniciar heartbeat DESPUÉS de que la UI esté configurada
        // Esto asegura que si hay registro pendiente, se complete primero
        initializeHeartbeat()

        // Iniciar polling de notificaciones
        initializeNotifications()
    }

    private fun initializeHeartbeat() {
        // Esperar un momento para que el registro se complete si es necesario
        window.decorView.post {
            Timber.i("HEARTBEAT: Verificando estado de registro antes de iniciar")

            // Verificar si el usuario ya completó el registro o lo omitió
            if (userPreferences.hasCompletedRegistration()) {
                Timber.i("HEARTBEAT: Usuario registrado, iniciando heartbeat con datos: ${userPreferences.userName ?: "N/A"}")
                heartbeatService.startHeartbeat()
            } else {
                Timber.i("HEARTBEAT: Registro pendiente, heartbeat se iniciará después del registro")
                // El heartbeat se iniciará desde RegistrationScreen cuando se complete
            }
        }
    }
    
    private fun initializeNotifications() {
        Timber.i("NOTIFICATIONS: Iniciando servicio de polling de notificaciones")
        notificationPollingService.startPolling()
    }

    private fun setupFullScreenForTV() {
        // Pantalla completa inmersiva
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        
        // Ocultar todas las barras del sistema
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        
        // Configuración adicional para TV
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        
        // Modo inmersivo sticky
        window.decorView.systemUiVisibility = (
            android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            or android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            or android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            or android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            or android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            or android.view.View.SYSTEM_UI_FLAG_FULLSCREEN
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        // Detener heartbeat cuando se destruye la actividad
        heartbeatService.stopHeartbeat()
        // Detener polling de notificaciones
        notificationPollingService.stopPolling()
        Timber.i("NOTIFICATIONS: Servicio de polling detenido")
    }
}
