package com.dms2350.iptvapp

import android.app.Application
import android.content.ComponentCallbacks2
import com.dms2350.iptvapp.data.service.DeviceHeartbeatService
import com.dms2350.iptvapp.utils.CrashHandler
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class IPTVApplication : Application() {

    @Inject
    lateinit var heartbeatService: DeviceHeartbeatService

    override fun onCreate() {
        super.onCreate()

        // Configurar Timber según el tipo de build
        if (BuildConfig.DEBUG) {
            // En DEBUG: logs completos con tags y líneas de código
            Timber.plant(Timber.DebugTree())
        } else {
            // En RELEASE: árbol que NO loguea nada (o puede enviar a Crashlytics)
            Timber.plant(object : Timber.Tree() {
                override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
                    // No hacer nada en producción
                    // Alternativa: enviar errores críticos a Firebase Crashlytics
                }
            })
        }

        // Configurar manejador de crashes
        Thread.setDefaultUncaughtExceptionHandler(CrashHandler(this))
        
        Timber.i("IPTV: Aplicación iniciada con protección anti-crash")

        // NO iniciar heartbeat aquí - se iniciará después del registro o cuando se acceda a canales
        Timber.i("HEARTBEAT: Servicio listo para iniciar después del registro")
    }

    /**
     * Se llama cuando el sistema libera memoria
     */
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)

        try {
            when (level) {
                // App en segundo plano y la UI ya no es visible
                TRIM_MEMORY_UI_HIDDEN -> {
                    Timber.i("HEARTBEAT: UI oculta, enviando heartbeat de estado")
                    heartbeatService.sendImmediateHeartbeat()
                }
                // App en segundo plano y el sistema está liberando memoria
                TRIM_MEMORY_BACKGROUND,
                TRIM_MEMORY_MODERATE,
                TRIM_MEMORY_COMPLETE -> {
                    Timber.i("HEARTBEAT: App en segundo plano, enviando heartbeat")
                    heartbeatService.sendImmediateHeartbeat()
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "HEARTBEAT: Error al manejar onTrimMemory")
            // No propagar la excepción
        }
    }

    /**
     * Se llama cuando la aplicación se está terminando
     * Nota: Este método NO se llama siempre en dispositivos reales
     */
    override fun onTerminate() {
        try {
            Timber.i("HEARTBEAT: App terminándose, enviando heartbeat final")
            heartbeatService.sendImmediateHeartbeat()
            heartbeatService.cleanup()
        } catch (e: Exception) {
            Timber.e(e, "HEARTBEAT: Error en onTerminate")
        } finally {
            super.onTerminate()
        }
    }

    /**
     * Se llama cuando el sistema está bajo presión de memoria crítica
     */
    override fun onLowMemory() {
        try {
            Timber.w("HEARTBEAT: Memoria baja, enviando heartbeat de emergencia")
            heartbeatService.sendImmediateHeartbeat()
        } catch (e: Exception) {
            Timber.e(e, "HEARTBEAT: Error en onLowMemory")
        } finally {
            super.onLowMemory()
        }
    }
}
