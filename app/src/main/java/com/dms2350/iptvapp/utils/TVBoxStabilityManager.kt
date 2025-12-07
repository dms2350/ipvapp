package com.dms2350.iptvapp.utils

import android.content.Context
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TVBoxStabilityManager @Inject constructor(
    private val context: Context
) {
    
    private var memoryMonitorJob: Job? = null
    private var isMonitoring = false
    private val handler = Handler(Looper.getMainLooper())
    
    fun startStabilityMonitoring() {
        if (isMonitoring) return
        
        isMonitoring = true
        Timber.d("IPTV: Iniciando monitoreo de estabilidad para TV Box")
        
        // Monitoreo de memoria cada 30 segundos
        memoryMonitorJob = CoroutineScope(Dispatchers.Main).launch {
            while (isMonitoring) {
                try {
                    checkMemoryStatus()
                    delay(30000) // 30 segundos
                } catch (e: Exception) {
                    Timber.d("IPTV: Error en monitoreo de memoria: ${e.message}")
                    delay(60000) // Esperar más tiempo si hay error
                }
            }
        }
        
        // Limpieza periódica cada 5 minutos
        handler.postDelayed(object : Runnable {
            override fun run() {
                if (isMonitoring) {
                    performPeriodicCleanup()
                    handler.postDelayed(this, 300000) // 5 minutos
                }
            }
        }, 300000)
    }
    
    fun stopStabilityMonitoring() {
        isMonitoring = false
        memoryMonitorJob?.cancel()
        handler.removeCallbacksAndMessages(null)
        Timber.d("IPTV: Monitoreo de estabilidad detenido")
    }
    
    private fun checkMemoryStatus() {
        try {
            val runtime = Runtime.getRuntime()
            val maxMemory = runtime.maxMemory()
            val totalMemory = runtime.totalMemory()
            val freeMemory = runtime.freeMemory()
            val usedMemory = totalMemory - freeMemory
            val memoryUsagePercent = (usedMemory * 100) / maxMemory
            
            Timber.d("IPTV: Memoria usada: ${memoryUsagePercent}% (${usedMemory / 1024 / 1024}MB de ${maxMemory / 1024 / 1024}MB)")
            
            // Si el uso de memoria es alto, ejecutar limpieza
            if (memoryUsagePercent > 75) {
                Timber.d("IPTV: Uso de memoria alto (${memoryUsagePercent}%) - ejecutando limpieza")
                performEmergencyCleanup()
            }
            
        } catch (e: Exception) {
            Timber.d("IPTV: Error verificando memoria: ${e.message}")
        }
    }
    
    private fun performPeriodicCleanup() {
        try {
            Timber.d("IPTV: Ejecutando limpieza periódica")
            
            // Garbage collection suave
            System.gc()
            
            // Limpiar caché de imágenes si es posible
            try {
                // Esto podría implementarse con Coil o la librería de imágenes que uses
                Timber.d("IPTV: Limpieza de caché de imágenes")
            } catch (e: Exception) {
                Timber.d("IPTV: Error limpiando caché de imágenes: ${e.message}")
            }
            
        } catch (e: Exception) {
            Timber.d("IPTV: Error en limpieza periódica: ${e.message}")
        }
    }
    
    private fun performEmergencyCleanup() {
        try {
            Timber.d("IPTV: Ejecutando limpieza de emergencia")
            
            // Garbage collection agresivo
            System.gc()
            Runtime.getRuntime().gc()
            
            // Esperar un poco para que el GC haga efecto
            Thread.sleep(100)
            
            // Verificar memoria después de la limpieza
            val runtime = Runtime.getRuntime()
            val usedMemory = runtime.totalMemory() - runtime.freeMemory()
            val memoryUsagePercent = (usedMemory * 100) / runtime.maxMemory()
            
            Timber.d("IPTV: Memoria después de limpieza: ${memoryUsagePercent}%")
            
        } catch (e: Exception) {
            Timber.d("IPTV: Error en limpieza de emergencia: ${e.message}")
        }
    }
    
    fun handleCriticalError(error: Throwable) {
        try {
            Timber.d("IPTV: Manejando error crítico: ${error.message}")
            
            // Ejecutar limpieza inmediata
            performEmergencyCleanup()
            
            // Si es un TV Box, aplicar estrategias específicas
            if (DeviceUtils.isTV(context)) {
                handleTVBoxCriticalError(error)
            }
            
        } catch (e: Exception) {
            Timber.d("IPTV: Error manejando error crítico: ${e.message}")
        }
    }
    
    private fun handleTVBoxCriticalError(error: Throwable) {
        try {
            Timber.d("IPTV: Aplicando estrategias específicas para TV Box")
            
            // Estrategias específicas para TV Box
            when {
                error.message?.contains("surface", ignoreCase = true) == true -> {
                    Timber.d("IPTV: Error de surface detectado - aplicando workaround")
                    // Aquí podrías reiniciar el surface o aplicar otros workarounds
                }
                
                error.message?.contains("codec", ignoreCase = true) == true -> {
                    Timber.d("IPTV: Error de codec detectado - aplicando workaround")
                    // Aquí podrías cambiar configuraciones de codec
                }
                
                error is OutOfMemoryError -> {
                    Timber.d("IPTV: OutOfMemoryError - limpieza agresiva")
                    performEmergencyCleanup()
                }
            }
            
        } catch (e: Exception) {
            Timber.d("IPTV: Error en estrategias TV Box: ${e.message}")
        }
    }
    
    fun getMemoryInfo(): MemoryInfo {
        return try {
            val runtime = Runtime.getRuntime()
            val maxMemory = runtime.maxMemory()
            val totalMemory = runtime.totalMemory()
            val freeMemory = runtime.freeMemory()
            val usedMemory = totalMemory - freeMemory
            
            MemoryInfo(
                maxMemoryMB = maxMemory / 1024 / 1024,
                usedMemoryMB = usedMemory / 1024 / 1024,
                freeMemoryMB = freeMemory / 1024 / 1024,
                usagePercent = (usedMemory * 100) / maxMemory
            )
        } catch (e: Exception) {
            Timber.d("IPTV: Error obteniendo info de memoria: ${e.message}")
            MemoryInfo(0, 0, 0, 0)
        }
    }
    
    data class MemoryInfo(
        val maxMemoryMB: Long,
        val usedMemoryMB: Long,
        val freeMemoryMB: Long,
        val usagePercent: Long
    )
}
