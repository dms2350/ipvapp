package com.dms2350.iptvapp.utils

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import com.dms2350.iptvapp.presentation.ui.player.VLCPlayerManager
import com.dms2350.iptvapp.utils.DeviceUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppLifecycleObserver @Inject constructor(
    private val vlcPlayerManager: VLCPlayerManager,
    @ApplicationContext private val context: Context
) : Application.ActivityLifecycleCallbacks {
    
    private var isAppInBackground = false
    private var activityCount = 0
    
    fun initialize(application: Application) {
        application.registerActivityLifecycleCallbacks(this)
        println("IPTV: AppLifecycleObserver inicializado")
    }
    
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        println("IPTV: Activity creada: ${activity.javaClass.simpleName}")
    }
    
    override fun onActivityStarted(activity: Activity) {
        activityCount++
        if (isAppInBackground) {
            isAppInBackground = false
            println("IPTV: App volvió al primer plano")
            
            // Reanudar reproducción si es TV Box
            if (DeviceUtils.isTV(context)) {
                try {
                    vlcPlayerManager.resumePlayback()
                    println("IPTV: Reproducción reanudada al volver al frente (TV Box)")
                } catch (e: Exception) {
                    println("IPTV: Error reanudando al volver al frente: ${e.message}")
                }
            }
        }
    }
    
    override fun onActivityResumed(activity: Activity) {
        println("IPTV: Activity reanudada: ${activity.javaClass.simpleName}")
    }
    
    override fun onActivityPaused(activity: Activity) {
        println("IPTV: Activity pausada: ${activity.javaClass.simpleName}")
    }
    
    override fun onActivityStopped(activity: Activity) {
        activityCount--
        if (activityCount == 0) {
            isAppInBackground = true
            println("IPTV: App fue al segundo plano")
            
            // Para TV Box, pausar reproducción cuando la app va al fondo
            if (DeviceUtils.isTV(context)) {
                try {
                    vlcPlayerManager.pausePlayback()
                    println("IPTV: Reproducción pausada por ir al fondo (TV Box)")
                } catch (e: Exception) {
                    println("IPTV: Error pausando en segundo plano: ${e.message}")
                }
            }
        }
    }
    
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
        // No necesario para nuestro caso
    }
    
    override fun onActivityDestroyed(activity: Activity) {
        println("IPTV: Activity destruida: ${activity.javaClass.simpleName}")
        
        // IMPORTANTE: NO liberar VLC automáticamente para evitar crashes en TV Box
        // Solo liberar si la app realmente se está cerrando completamente
        if (activity.javaClass.simpleName == "MainActivity" && activity.isFinishing) {
            try {
                println("IPTV: MainActivity finalizando - liberación segura de VLC")
                
                // Para TV Box Android 11/12, liberar de forma más segura
                if (DeviceUtils.isTV(context) && android.os.Build.VERSION.SDK_INT >= 30) {
                    println("IPTV: Liberación especial para TV Box Android 11+")
                    
                    // Detener reproducción primero
                    vlcPlayerManager.stop()
                    
                    // Esperar un poco para que se liberen las surfaces
                    Thread.sleep(1000)
                    
                    // Luego liberar completamente
                    vlcPlayerManager.release()
                } else {
                    vlcPlayerManager.release()
                }
                
                println("IPTV: VLC liberado por destrucción de MainActivity")
            } catch (e: Exception) {
                println("IPTV: Error liberando VLC en destrucción: ${e.message}")
                // No propagar el error para evitar crash
            }
        } else {
            println("IPTV: MainActivity no está finalizando - VLC se mantiene activo")
        }
    }
    
    fun isAppInBackground(): Boolean = isAppInBackground
}