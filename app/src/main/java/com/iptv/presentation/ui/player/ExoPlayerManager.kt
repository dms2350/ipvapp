package com.iptv.presentation.ui.player

import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.datasource.DefaultHttpDataSource
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExoPlayerManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var _exoPlayer: ExoPlayer? = null
    private var currentStreamUrl: String? = null

    val exoPlayer: ExoPlayer
        get() = _exoPlayer ?: createPlayer()

    private fun createPlayer(): ExoPlayer {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
            .build()
            
        val builder = ExoPlayer.Builder(context)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
        
        return builder.build().also {
            _exoPlayer = it
            it.volume = 1.0f
            it.playWhenReady = true
            
            it.addListener(object : Player.Listener {
                override fun onPlayerError(error: PlaybackException) {
                    println("IPTV: Error de reproducción: ${error.message}")
                }
                
                override fun onPlaybackStateChanged(playbackState: Int) {
                    val stateText = when (playbackState) {
                        Player.STATE_IDLE -> "IDLE"
                        Player.STATE_BUFFERING -> "BUFFERING"
                        Player.STATE_READY -> "READY"
                        Player.STATE_ENDED -> "ENDED"
                        else -> "UNKNOWN"
                    }
                    println("IPTV: Estado: $stateText")
                }
            })
        }
    }

    fun playStream(streamUrl: String) {
        currentStreamUrl = streamUrl
        println("IPTV: Reproduciendo: $streamUrl")
        startPlayback(streamUrl)
    }
    
    private fun startPlayback(streamUrl: String) {
        try {
            val dataSourceFactory = DefaultHttpDataSource.Factory()
                .setConnectTimeoutMs(10000)
                .setReadTimeoutMs(10000)
                .setAllowCrossProtocolRedirects(true)
            
            if (streamUrl.contains(".m3u8")) {
                val mediaSource = HlsMediaSource.Factory(dataSourceFactory)
                    .createMediaSource(MediaItem.fromUri(streamUrl))
                    
                exoPlayer.apply {
                    setMediaSource(mediaSource)
                    prepare()
                    playWhenReady = true
                }
            } else {
                val mediaItem = MediaItem.fromUri(streamUrl)
                exoPlayer.apply {
                    setMediaItem(mediaItem)
                    prepare()
                    playWhenReady = true
                }
            }
        } catch (e: Exception) {
            println("IPTV: Error: ${e.message}")
        }
    }

    fun pause() {
        exoPlayer.pause()
    }

    fun resume() {
        exoPlayer.play()
    }

    fun stop() {
        exoPlayer.stop()
    }

    fun release() {
        _exoPlayer?.release()
        _exoPlayer = null
        currentStreamUrl = null
    }
    
    fun forceReconnect() {
        currentStreamUrl?.let { url ->
            startPlayback(url)
        }
    }
}