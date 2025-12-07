package com.dms2350.iptvapp.data.service

import android.annotation.SuppressLint
import android.app.UiModeManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.provider.Settings
import com.dms2350.iptvapp.BuildConfig
import com.dms2350.iptvapp.data.api.IPTVApi
import com.dms2350.iptvapp.data.api.dto.DeviceInfoDto
import com.dms2350.iptvapp.data.local.UserPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceHeartbeatService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val api: IPTVApi,
    private val userPreferences: UserPreferences
) {
    private var heartbeatJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    companion object {
        private const val HEARTBEAT_INTERVAL_MS = 60_000L // 60 segundos
        private const val INITIAL_DELAY_MS = 5_000L // 5 segundos de delay inicial
    }

    /**
     * Inicia el servicio de heartbeat que enviará información del dispositivo periódicamente
     */
    fun startHeartbeat() {
        if (heartbeatJob?.isActive == true) {
            Timber.d("HEARTBEAT: Servicio ya está activo")
            return
        }

        Timber.i("HEARTBEAT: Iniciando servicio de heartbeat")

        heartbeatJob = scope.launch {
            // Delay inicial para dar tiempo a que la app se inicialice completamente
            delay(INITIAL_DELAY_MS)

            while (isActive) {
                try {
                    sendHeartbeat()
                } catch (e: Exception) {
                    Timber.e(e, "HEARTBEAT: Error al enviar heartbeat")
                }

                // Esperar antes del próximo heartbeat
                delay(HEARTBEAT_INTERVAL_MS)
            }
        }
    }

    /**
     * Detiene el servicio de heartbeat
     */
    fun stopHeartbeat() {
        Timber.i("HEARTBEAT: Deteniendo servicio de heartbeat")
        heartbeatJob?.cancel()
        heartbeatJob = null
    }

    /**
     * Envía un heartbeat inmediato sin esperar el intervalo programado
     */
    suspend fun sendHeartbeat() {
        try {
            val deviceInfo = collectDeviceInfo()

            Timber.d("HEARTBEAT: Enviando información del dispositivo: ID=${deviceInfo.deviceId}, Type=${deviceInfo.deviceType}")

            val response = withTimeoutOrNull(10_000L) {
                api.sendHeartbeat(deviceInfo)
            }

            if (response == null) {
                Timber.w("HEARTBEAT: Timeout al enviar heartbeat")
                return
            }

            if (response.isSuccessful) {
                val body = response.body()
                if (body?.success == true) {
                    Timber.d("HEARTBEAT: Enviado exitosamente")
                } else {
                    Timber.w("HEARTBEAT: Respuesta no exitosa: ${body?.message}")
                }
            } else {
                Timber.e("HEARTBEAT: Error HTTP ${response.code()}: ${response.message()}")
            }
        } catch (e: Exception) {
            Timber.e(e, "HEARTBEAT: Error al enviar heartbeat")
            // No propagar la excepción para evitar crashes
        }
    }

    /**
     * Envía un heartbeat inmediato de forma síncrona (útil para cuando la app se cierra)
     */
    fun sendImmediateHeartbeat() {
        try {
            scope.launch {
                try {
                    sendHeartbeat()
                    Timber.i("HEARTBEAT: Heartbeat final enviado (app cerrándose)")
                } catch (e: Exception) {
                    Timber.e(e, "HEARTBEAT: Error al enviar heartbeat final")
                    // No propagar la excepción para evitar crashes
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "HEARTBEAT: Error crítico al lanzar coroutine de heartbeat")
            // No propagar la excepción
        }
    }

    /**
     * Recopila toda la información del dispositivo
     */
    @SuppressLint("HardwareIds")
    private fun collectDeviceInfo(): DeviceInfoDto {
        val deviceId = getDeviceId()
        val deviceType = getDeviceType()
        val manufacturer = Build.MANUFACTURER
        val model = Build.MODEL
        val osVersion = Build.VERSION.RELEASE
        val sdkInt = Build.VERSION.SDK_INT
        val appVersion = BuildConfig.VERSION_NAME

        // Obtener información del usuario si existe
        // Si es null, enviar "N/A" en lugar de null para evitar registros duplicados
        val userName = userPreferences.userName ?: "N/A"
        val userCedula = userPreferences.userCedula ?: "N/A"

        Timber.d("HEARTBEAT: Recopilando info - DeviceID: $deviceId, User: $userName, Cedula: $userCedula")

        return DeviceInfoDto(
            deviceId = deviceId,
            deviceType = deviceType,
            manufacturer = manufacturer,
            model = model,
            osVersion = osVersion,
            sdkInt = sdkInt,
            appVersion = appVersion,
            userFullName = userName,
            userIdNumber = userCedula
        )
    }

    /**
     * Obtiene el ANDROID_ID del dispositivo
     */
    @SuppressLint("HardwareIds")
    private fun getDeviceId(): String {
        return try {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
                ?: "unknown_${System.currentTimeMillis()}"
        } catch (e: Exception) {
            Timber.e(e, "HEARTBEAT: Error al obtener ANDROID_ID")
            "unknown_${System.currentTimeMillis()}"
        }
    }

    /**
     * Detecta el tipo de dispositivo (TV, TV_BOX, PHONE, OTHER)
     */
    private fun getDeviceType(): String {
        return try {
            val uiModeManager = context.getSystemService(Context.UI_MODE_SERVICE) as? UiModeManager

            when (uiModeManager?.currentModeType) {
                Configuration.UI_MODE_TYPE_TELEVISION -> {
                    // Diferenciar entre TV y TV Box
                    if (isTVBox()) {
                        "TV_BOX"
                    } else {
                        "TV"
                    }
                }
                Configuration.UI_MODE_TYPE_WATCH -> "WATCH"
                Configuration.UI_MODE_TYPE_CAR -> "CAR"
                Configuration.UI_MODE_TYPE_DESK -> "DESK"
                Configuration.UI_MODE_TYPE_NORMAL -> {
                    // Podría ser teléfono o tablet
                    if (isTablet()) {
                        "TABLET"
                    } else {
                        "PHONE"
                    }
                }
                else -> "OTHER"
            }
        } catch (e: Exception) {
            Timber.e(e, "HEARTBEAT: Error al detectar tipo de dispositivo")
            "OTHER"
        }
    }

    /**
     * Detecta si es un TV Box basándose en características del dispositivo
     */
    private fun isTVBox(): Boolean {
        val manufacturer = Build.MANUFACTURER.lowercase()
        val model = Build.MODEL.lowercase()
        val product = Build.PRODUCT.lowercase()

        val tvBoxIndicators = listOf(
            "box", "stick", "dongle", "rockchip", "amlogic",
            "allwinner", "mediatek", "h96", "x96", "t95", "tx"
        )

        return tvBoxIndicators.any {
            manufacturer.contains(it) || model.contains(it) || product.contains(it)
        }
    }

    /**
     * Detecta si es una tablet basándose en el tamaño de pantalla
     */
    private fun isTablet(): Boolean {
        return try {
            val configuration = context.resources.configuration
            val screenLayout = configuration.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK

            screenLayout >= Configuration.SCREENLAYOUT_SIZE_LARGE
        } catch (e: Exception) {
            Timber.e(e, "HEARTBEAT: Error al detectar si es tablet")
            false
        }
    }

    /**
     * Limpia los recursos cuando se destruye el servicio
     */
    fun cleanup() {
        stopHeartbeat()
        scope.cancel()
        Timber.i("HEARTBEAT: Servicio limpiado")
    }
}

