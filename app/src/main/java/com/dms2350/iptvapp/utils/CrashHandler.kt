package com.dms2350.iptvapp.utils

import android.app.AlertDialog
import android.content.Context
import android.os.Handler
import android.os.Looper
import timber.log.Timber

class CrashHandler(private val context: Context) : Thread.UncaughtExceptionHandler {
    
    private val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
    
    override fun uncaughtException(thread: Thread, exception: Throwable) {
        try {
            Timber.e(exception, "IPTV: Crash interceptado en thread: ${thread.name}")
            exception.printStackTrace()
            
            // Si es un error de VLC o de red, intentar recuperarse
            if (isRecoverableError(exception)) {
                Timber.i("IPTV: Error recuperable detectado - no cerrando la app")

                // Mostrar diálogo informativo (solo en modo debug)
                Handler(Looper.getMainLooper()).post {
                    try {
                        showErrorDialog(exception)
                    } catch (e: Exception) {
                        Timber.e(e, "IPTV: Error mostrando diálogo de error")
                    }
                }
                return
            }
            
            // Para errores críticos no recuperables, usar el handler por defecto
            Timber.e("IPTV: Error crítico - cerrando app")
            defaultHandler?.uncaughtException(thread, exception)
            
        } catch (e: Exception) {
            // Si falla el manejo del crash, usar handler por defecto
            Timber.e(e, "IPTV: Error en el crash handler")
            defaultHandler?.uncaughtException(thread, exception)
        }
    }
    
    private fun isRecoverableError(exception: Throwable): Boolean {
        val message = exception.message?.lowercase() ?: ""
        val stackTrace = exception.stackTraceToString().lowercase()
        
        return message.contains("vlc") || 
               message.contains("libvlc") ||
               message.contains("media") ||
               message.contains("network") ||
               message.contains("timeout") ||
               message.contains("connection") ||
               message.contains("socket") ||
               stackTrace.contains("vlc") ||
               stackTrace.contains("libvlc") ||
               stackTrace.contains("mediaplayer") ||
               stackTrace.contains("retrofit") ||
               stackTrace.contains("okhttp")
    }

    private fun showErrorDialog(exception: Throwable) {
        try {
            AlertDialog.Builder(context)
                .setTitle("Error Temporal")
                .setMessage("Se produjo un error recuperable:\n${exception.message}\n\nLa aplicación continuará funcionando.")
                .setPositiveButton("OK") { dialog, _ ->
                    dialog.dismiss()
                }
                .setCancelable(true)
                .show()
        } catch (e: Exception) {
            Timber.e(e, "IPTV: No se pudo mostrar diálogo de error")
        }
    }
}
