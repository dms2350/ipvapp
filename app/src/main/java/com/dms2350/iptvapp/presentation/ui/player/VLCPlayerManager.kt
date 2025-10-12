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
    private val problematicChannels = mutableSetOf<String>()
    private var errorCount = 0

    val mediaPlayer: MediaPlayer
        get() = _mediaPlayer ?: createPlayer()

    private fun createPlayer(): MediaPlayer {
        try {
            val options = arrayListOf<String>()
            options.add("--aout=opensles")
            options.add("--audio-time-stretch")
            options.add("--network-caching=3000")
            options.add("--live-caching=1000")
            
            _libVLC = LibVLC(context, options)
            _mediaPlayer = MediaPlayer(_libVLC)
            
            // Agregar listener de errores
            _mediaPlayer!!.setEventListener { event ->
                when (event.type) {
                    MediaPlayer.Event.EncounteredError -> {
                        println("VLC: Error en reproducción - saltando al siguiente canal")
                        currentStreamUrl?.let { url ->
                            problematicChannels.add(url)
                            println("VLC: Canal agregado a lista negra: $url")
                        }
                        errorCount++
                        isChangingChannel = false
                        
                        // Recrear player si hay muchos errores
                        if (errorCount >= 3) {
                            println("VLC: Recreando player por exceso de errores")
                            recreatePlayer()
                        }
                        
                        onChannelError?.invoke()
                    }
                    MediaPlayer.Event.Playing -> {
                        println("VLC: Reproducción iniciada exitosamente")
                        isChangingChannel = false // Canal funcionando, liberar lock
                    }
                    MediaPlayer.Event.EndReached -> {
                        println("VLC: Stream terminado")
                        isChangingChannel = false
                    }
                    MediaPlayer.Event.Stopped -> {
                        println("VLC: Reproducción detenida")
                        isChangingChannel = false
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
            println("VLC: Cambio de canal en progreso, ignorando...")
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
            
            // Crear nuevo media con validación
            val media = Media(_libVLC, Uri.parse(streamUrl))
            mediaPlayer.media = media
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
}
