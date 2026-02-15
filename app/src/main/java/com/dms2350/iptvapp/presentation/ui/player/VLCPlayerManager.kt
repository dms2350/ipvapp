package com.dms2350.iptvapp.presentation.ui.player

import android.content.Context
import android.net.Uri
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VLCPlayerManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var _libVLC: LibVLC? = null
    private var _mediaPlayer: MediaPlayer? = null
    private var currentStreamUrl: String? = null
    private var isChangingChannel = false
    private var onChannelError: (() -> Unit)? = null
    private var onBufferingIssue: (() -> Unit)? = null
    private var onBufferingStart: (() -> Unit)? = null
    private val problematicChannels = mutableSetOf<String>()
    private var errorCount = 0
    private var bufferingStartTime: Long = 0
    private var isBuffering = false
    private var bufferingNotified = false
    private var isPlaybackStable = false // Nueva variable para saber si ya reprodujo bien
    private var lastFixTime: Long = 0 // Timestamp del último fix aplicado
    private val FIX_COOLDOWN = 5000L // 5 segundos entre fixes

    val mediaPlayer: MediaPlayer
        get() = _mediaPlayer ?: createPlayer()

    private fun createPlayer(): MediaPlayer {
        try {
            val options = arrayListOf<String>()
            
            // === CONFIGURACIÓN DE BUFFER (GLOBAL) ===
            options.add("--network-caching=3000")  // 3 segundos para WiFi inestable
            options.add("--live-caching=2000")     // 2 segundos para streams en vivo
            
            // === CONFIGURACIÓN DE RECONEXIÓN AUTOMÁTICA ===
            options.add("--http-reconnect")        // Reconexión automática HTTP
            options.add("--adaptive-logic=highest") // Mejor calidad adaptativa
            options.add("--adaptive-maxwidth=1920") // Max resolución
            
            // === CONFIGURACIÓN DE AUDIO/VIDEO ===
            options.add("--avcodec-hw=any")        // Hardware decoding con fallback a software
            options.add("--aout=opensles")         // OpenSLES para audio compatible
            options.add("--vout=android_display")  // Salida de video estándar Android
            options.add("--audio-time-stretch")    // Permite ajuste de velocidad de audio
            
            // === CONFIGURACIÓN DE SINCRONIZACIÓN ===
            options.add("--clock-jitter=0")        // Desactiva corrección de jitter
            options.add("--clock-synchro=0")       // Desactiva auto-sincronización
            
            println("VLC: Creando player con reconexión automática y buffer optimizado")
            println("VLC: Buffer: network=3000ms, live=2000ms")
            println("VLC: Reconexión HTTP automática habilitada")

            _libVLC = LibVLC(context, options)
            _mediaPlayer = MediaPlayer(_libVLC)
            
            // Agregar listener de errores
            _mediaPlayer!!.setEventListener { event ->
                when (event.type) {
                    MediaPlayer.Event.Buffering -> {
                        val bufferPercent = event.buffering
                        if (bufferPercent < 100f) {
                            if (!isBuffering) {
                                bufferingStartTime = System.currentTimeMillis()
                                isBuffering = true
                                bufferingNotified = false
                                println("VLC: Buffering iniciado (${bufferPercent}%)")
                                
                                // Verificar si está en cooldown después de un fix
                                val timeSinceLastFix = System.currentTimeMillis() - lastFixTime
                                val inCooldown = timeSinceLastFix < FIX_COOLDOWN
                                
                                // Solo notificar inicio si la reproducción ya estaba estable Y no está en cooldown
                                if (isPlaybackStable && !inCooldown) {
                                    println("VLC: Re-buffering detectado (reproducción estaba estable)")
                                    lastFixTime = System.currentTimeMillis() // Actualizar timestamp
                                    onBufferingStart?.invoke()
                                } else if (inCooldown) {
                                    println("VLC: Buffering post-fix detectado - en cooldown (${timeSinceLastFix}ms/${FIX_COOLDOWN}ms)")
                                } else {
                                    println("VLC: Buffering inicial - no intervenir")
                                }
                            } else {
                                val bufferingDuration = System.currentTimeMillis() - bufferingStartTime
                                println("VLC: Buffering... ${bufferPercent}% (${bufferingDuration}ms)")
                                
                                // Verificar cooldown también para buffering prolongado
                                val timeSinceLastFix = System.currentTimeMillis() - lastFixTime
                                val inCooldown = timeSinceLastFix < FIX_COOLDOWN
                                
                                // Si lleva más de 3 segundos en buffering Y no está en cooldown, notificar
                                if (bufferingDuration > 3000 && !bufferingNotified && !inCooldown) {
                                    println("VLC: BUFFERING PROLONGADO (${bufferingDuration}ms) - Notificando")
                                    lastFixTime = System.currentTimeMillis()
                                    onBufferingIssue?.invoke()
                                    bufferingNotified = true
                                } else if (bufferingDuration > 3000 && inCooldown) {
                                    println("VLC: Buffering prolongado pero en cooldown - esperando (${timeSinceLastFix}ms/${FIX_COOLDOWN}ms)")
                                }
                            }
                        } else {
                            if (isBuffering) {
                                val bufferingDuration = System.currentTimeMillis() - bufferingStartTime
                                println("VLC: Buffering completado (duró ${bufferingDuration}ms)")
                                isBuffering = false
                                bufferingNotified = false
                            }
                        }
                    }
                    MediaPlayer.Event.EndReached -> {
                        println("VLC: Stream terminado")
                        isChangingChannel = false
                        isBuffering = false
                        bufferingNotified = false
                        isPlaybackStable = false
                    }
                    MediaPlayer.Event.EncounteredError -> {
                         println("VLC: Error detectado en reproducción")
                         isChangingChannel = false
                         isBuffering = false
                         bufferingNotified = false
                         isPlaybackStable = false
                         onChannelError?.invoke()
                    }
                    MediaPlayer.Event.Playing -> {
                        println("VLC: Reproducción iniciada exitosamente")
                        isChangingChannel = false // Canal funcionando, liberar lock
                        isBuffering = false
                        bufferingNotified = false
                        
                        // Marcar reproducción como estable después de 2 segundos
                        CoroutineScope(Dispatchers.IO).launch {
                            delay(2000)
                            isPlaybackStable = true
                            println("VLC: Reproducción estabilizada - monitoreando re-buffering")
                        }
                        
                        // Limpiar lista negra cuando un canal reproduce bien
                        if (problematicChannels.isNotEmpty()) {
                            println("VLC: Canal reprodujo exitosamente - limpiando lista negra (${problematicChannels.size} canales)")
                            problematicChannels.clear()
                            println("VLC: Lista negra limpiada - todos los canales disponibles nuevamente")
                        }
                    }
                    MediaPlayer.Event.Stopped -> {
                        println("VLC: Reproducción detenida")
                        isChangingChannel = false
                        isBuffering = false
                        bufferingNotified = false
                        isPlaybackStable = false
                    }
                    MediaPlayer.Event.ESAdded -> {
                        println("VLC: Nueva pista de stream detectada (video/audio)")
                        // Verificar cooldown
                        val timeSinceLastFix = System.currentTimeMillis() - lastFixTime
                        val inCooldown = timeSinceLastFix < FIX_COOLDOWN
                        
                        // Solo aplicar fix si ya estaba estable Y no está en cooldown
                        // Y ha pasado suficiente tiempo desde que inició la reproducción
                        if (isPlaybackStable && !inCooldown && timeSinceLastFix > 10000) {
                            println("VLC: Cambio de formato detectado después de reproducción estable - Aplicando fix preventivo")
                            lastFixTime = System.currentTimeMillis()
                            onBufferingStart?.invoke()
                        } else if (inCooldown) {
                            println("VLC: Nueva pista detectada pero en cooldown - ignorando (${timeSinceLastFix}ms/${FIX_COOLDOWN}ms)")
                        } else {
                            println("VLC: Nueva pista detectada durante inicialización - normal")
                        }
                    }
                    MediaPlayer.Event.ESDeleted -> {
                        println("VLC: Pista de stream eliminada")
                        // Verificar cooldown
                        val timeSinceLastFix = System.currentTimeMillis() - lastFixTime
                        val inCooldown = timeSinceLastFix < FIX_COOLDOWN
                        
                        // Solo aplicar fix si ya estaba estable Y no está en cooldown
                        if (isPlaybackStable && !inCooldown && timeSinceLastFix > 10000) {
                            println("VLC: Pérdida de pista durante reproducción estable - Aplicando fix")
                            lastFixTime = System.currentTimeMillis()
                            onBufferingStart?.invoke()
                        } else if (inCooldown) {
                            println("VLC: Pista eliminada pero en cooldown - ignorando (${timeSinceLastFix}ms/${FIX_COOLDOWN}ms)")
                        } else {
                            println("VLC: Pista eliminada durante inicialización - normal")
                        }
                    }
                    MediaPlayer.Event.Vout -> {
                        val voutCount = event.voutCount
                        println("VLC: Video output cambió: $voutCount salidas")
                        
                        // Verificar cooldown
                        val timeSinceLastFix = System.currentTimeMillis() - lastFixTime
                        val inCooldown = timeSinceLastFix < FIX_COOLDOWN
                        
                        // Solo actuar si video output se perdió (0) durante reproducción estable
                        if (voutCount == 0 && isPlaybackStable && !inCooldown && timeSinceLastFix > 10000) {
                            println("VLC: Video output perdido durante reproducción - posible congelamiento")
                            lastFixTime = System.currentTimeMillis()
                            onBufferingStart?.invoke()
                        } else if (voutCount == 0 && inCooldown) {
                            println("VLC: Video output perdido pero en cooldown - ignorando")
                        }
                    }
                }
            }
            
            println("VLC: Player creado exitosamente")
            return _mediaPlayer!!
        } catch (e: Exception) {
            println("VLC: Error creando player: ${e.message}")
            isChangingChannel = false
            throw e
        }
    }

    fun playStream(streamUrl: String) {
        if (isChangingChannel) {
            println("VLC: Cambio de formato o canal en progreso, ignorando...")
            return
        }
        
        // Verificar si el canal está en lista negra
        if (problematicChannels.contains(streamUrl)) {
            println("VLC: Canal en lista negra, saltando automáticamente")
            onChannelError?.invoke()
            return
        }
        
        isChangingChannel = true
        currentStreamUrl = streamUrl
        isPlaybackStable = false // Resetear flag al cambiar de canal
        lastFixTime = 0 // Resetear cooldown
        println("VLC: Reproduciendo: $streamUrl")

        try {
            // Detener reproducción anterior de forma segura
            try {
                if (mediaPlayer.isPlaying) {
                    mediaPlayer.stop()
                }
            } catch (e: Exception) {
                println("VLC: Error deteniendo reproducción anterior: ${e.message}")
            }
            
            // Crear nuevo media (configuración global se aplica automáticamente)
            val uri = Uri.parse(streamUrl)
            val media = Media(_libVLC, uri)
            
            println("VLC: Media creado para: $streamUrl")

            mediaPlayer.media = media
            // CRÍTICO: Liberar el objeto Media nativo inmediatamente después de asignarlo
            // Esto evita el crash "finalized but not natively released"
            media.release()
            
            // Iniciar reproducción
            mediaPlayer.play()
            
            // Timeout más largo para canales desde menú principal
            CoroutineScope(Dispatchers.Main).launch {
                delay(15000) // 15 segundos timeout para dar más tiempo
                if (isChangingChannel) {
                    println("VLC: Timeout después de 15 segundos - saltando al siguiente canal")
                    currentStreamUrl?.let { url ->
                        problematicChannels.add(url)
                        println("VLC: Canal agregado a lista negra por timeout: $url")
                    }
                    isChangingChannel = false
                    onChannelError?.invoke()
                }
            }
            
            println("VLC: Stream iniciado")
        } catch (e: Exception) {
            println("VLC: Error reproduciendo: ${e.message}")
            isChangingChannel = false
            // No propagar la excepción para evitar crash
            try {
                onChannelError?.invoke()
            } catch (callbackError: Exception) {
                println("VLC: Error en callback: ${callbackError.message}")
            }
        }
    }

    fun pause() {
        mediaPlayer.pause()
    }

    fun resume() {
        mediaPlayer.play()
    }

    fun stop() {
        try {
            if (mediaPlayer.isPlaying) {
                mediaPlayer.stop()
            }
        } catch (e: Exception) {
            println("VLC: Error deteniendo: ${e.message}")
            // Silenciar error para evitar crash
        }
    }

    fun release() {
        try {
            _mediaPlayer?.stop()
            _mediaPlayer?.release()
            _libVLC?.release()
        } catch (e: Exception) {
            println("VLC: Error liberando recursos: ${e.message}")
            // Silenciar error para evitar crash
        } finally {
            _mediaPlayer = null
            _libVLC = null
            currentStreamUrl = null
            isChangingChannel = false
        }
    }
    
    fun forceReconnect() {
        currentStreamUrl?.let { url ->
            playStream(url)
        }
    }
    
    fun setVolume(volume: Int) {
        mediaPlayer.volume = volume
    }
    
    fun getVolume(): Int {
        return mediaPlayer.volume
    }
    
    fun pausePlayback() {
        try {
            _mediaPlayer?.pause()
            println("VLC: Pausado")
        } catch (e: Exception) {
            println("VLC: Error pausando: ${e.message}")
        }
    }
    
    fun resumePlayback() {
        try {
            _mediaPlayer?.play()
            println("VLC: Reanudado")
        } catch (e: Exception) {
            println("VLC: Error reanudando: ${e.message}")
        }
    }
    
    fun setOnChannelErrorListener(listener: () -> Unit) {
        onChannelError = listener
    }
    
    fun setOnBufferingIssueListener(listener: () -> Unit) {
        onBufferingIssue = listener
    }
    
    fun setOnBufferingStartListener(listener: () -> Unit) {
        onBufferingStart = listener
    }
    
    private fun recreatePlayer() {
        try {
            println("VLC: Recreando player...")
            
            // Liberar player actual
            _mediaPlayer?.stop()
            _mediaPlayer?.release()
            _libVLC?.release()
            
            // Crear nuevo player
            _mediaPlayer = null
            _libVLC = null
            errorCount = 0
            
            // El nuevo player se creará automáticamente en la próxima llamada
            println("VLC: Player recreado exitosamente")
            
        } catch (e: Exception) {
            println("VLC: Error recreando player: ${e.message}")
        }
    }
    
    fun clearBlacklist() {
        problematicChannels.clear()
        println("VLC: Lista negra limpiada")
    }

    private fun restartStream() {
        val url = currentStreamUrl ?: return

        // Usar Handler principal para reiniciar
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            try {
                println("VLC: Reiniciando stream...")
                playStream(url)
                // Resetear flag de cambio porque playStream lo pone en true y si falló antes podría haber quedado
                isChangingChannel = false
                // playStream pondrá isChangingChannel a true, pero aquí lo llamamos recursivamente (cuidado)
                // Mejor llamamos a una versión interna o reseteamos flags.
                // playStream tiene chequeo de isChangingChannel...
                // Forzamos reset antes de llamar
            } catch (e: Exception) {
                println("VLC: Error al reiniciar stream: ${e.message}")
            }
        }, 1000)
    }
}
