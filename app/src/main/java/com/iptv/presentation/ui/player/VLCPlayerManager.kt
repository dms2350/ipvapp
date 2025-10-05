package com.iptv.presentation.ui.player

import android.content.Context
import android.net.Uri
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VLCPlayerManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var _libVLC: LibVLC? = null
    private var _mediaPlayer: MediaPlayer? = null
    private var currentStreamUrl: String? = null

    val mediaPlayer: MediaPlayer
        get() = _mediaPlayer ?: createPlayer()

    private fun createPlayer(): MediaPlayer {
        try {
            val options = arrayListOf<String>()
            options.add("--aout=opensles")
            options.add("--audio-time-stretch")
            options.add("-vvv")
            
            _libVLC = LibVLC(context, options)
            _mediaPlayer = MediaPlayer(_libVLC)
            
            println("VLC: Player creado exitosamente")
            return _mediaPlayer!!
        } catch (e: Exception) {
            println("VLC: Error creando player: ${e.message}")
            throw e
        }
    }

    fun playStream(streamUrl: String) {
        currentStreamUrl = streamUrl
        println("VLC: Reproduciendo: $streamUrl")
        
        try {
            val media = Media(_libVLC, Uri.parse(streamUrl))
            mediaPlayer.media = media
            media.release()
            mediaPlayer.play()
            
            println("VLC: Stream iniciado")
        } catch (e: Exception) {
            println("VLC: Error reproduciendo: ${e.message}")
        }
    }

    fun pause() {
        mediaPlayer.pause()
    }

    fun resume() {
        mediaPlayer.play()
    }

    fun stop() {
        mediaPlayer.stop()
    }

    fun release() {
        _mediaPlayer?.release()
        _libVLC?.release()
        _mediaPlayer = null
        _libVLC = null
        currentStreamUrl = null
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
}