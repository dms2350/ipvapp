package com.iptv.utils

import android.content.Context
import kotlin.system.exitProcess

class CrashHandler(private val context: Context) : Thread.UncaughtExceptionHandler {
    
    private val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
    
    override fun uncaughtException(thread: Thread, exception: Throwable) {
        try {
            println("IPTV: Crash interceptado: ${exception.message}")
            exception.printStackTrace()
            
            // Si es un error de VLC, no cerrar la app
            if (isVLCRelatedError(exception)) {
                println("IPTV: Error de VLC - manteniendo app abierta")
                return
            }
            
            // Para otros errores críticos, usar el handler por defecto
            defaultHandler?.uncaughtException(thread, exception)
            
        } catch (e: Exception) {
            // Si falla el manejo del crash, usar handler por defecto
            defaultHandler?.uncaughtException(thread, exception)
        }
    }
    
    private fun isVLCRelatedError(exception: Throwable): Boolean {
        val message = exception.message?.lowercase() ?: ""
        val stackTrace = exception.stackTraceToString().lowercase()
        
        return message.contains("vlc") || 
               message.contains("libvlc") ||
               message.contains("media") ||
               stackTrace.contains("vlc") ||
               stackTrace.contains("libvlc") ||
               stackTrace.contains("mediaplayer")
    }
}