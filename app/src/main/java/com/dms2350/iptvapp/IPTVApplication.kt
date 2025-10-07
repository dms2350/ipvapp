package com.dms2350.iptvapp

import android.app.Application
import com.dms2350.iptvapp.utils.CrashHandler
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class IPTVApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Configurar manejador de crashes
        Thread.setDefaultUncaughtExceptionHandler(CrashHandler(this))
        
        println("IPTV: Aplicación iniciada con protección anti-crash")
    }
}
