package com.iptv.utils

import android.content.Context
import android.media.AudioManager
import android.os.Build
import androidx.media3.common.C
import androidx.media3.common.Format

object AudioCodecHelper {
    
    // Códecs problemáticos conocidos
    private val PROBLEMATIC_CODECS = setOf(
        "ac-3",      // Dolby Digital
        "eac-3",     // Dolby Digital Plus
        "dts",       // DTS
        "truehd",    // Dolby TrueHD
        "mlp",       // MLP Lossless
        "pcm_s24le", // PCM 24-bit
        "pcm_s32le"  // PCM 32-bit
    )
    
    // Códecs compatibles universalmente
    private val COMPATIBLE_CODECS = setOf(
        "aac",
        "mp3",
        "opus",
        "vorbis",
        "pcm_s16le"
    )
    
    // Dispositivos con problemas conocidos de audio
    private val PROBLEMATIC_DEVICES = setOf(
        "xiaomi",
        "huawei", 
        "honor",
        "premier",
        "rockchip",
        "allwinner",
        "amlogic"
    )
    
    fun isCodecProblematic(codec: String?): Boolean {
        if (codec == null) return false
        return PROBLEMATIC_CODECS.any { 
            codec.contains(it, ignoreCase = true) 
        }
    }
    
    fun isCodecCompatible(codec: String?): Boolean {
        if (codec == null) return false
        return COMPATIBLE_CODECS.any { 
            codec.contains(it, ignoreCase = true) 
        }
    }
    
    fun isDeviceProblematic(): Boolean {
        val manufacturer = Build.MANUFACTURER.lowercase()
        val model = Build.MODEL.lowercase()
        
        return PROBLEMATIC_DEVICES.any { device ->
            manufacturer.contains(device) || model.contains(device)
        }
    }
    
    fun getOptimalAudioConfiguration(context: Context): AudioConfiguration {
        val isProblematicDevice = isDeviceProblematic()
        val isTV = context.packageManager.hasSystemFeature("android.software.leanback")
        
        return AudioConfiguration(
            usage = if (isProblematicDevice) C.USAGE_VOICE_COMMUNICATION else C.USAGE_MEDIA,
            contentType = if (isProblematicDevice) C.AUDIO_CONTENT_TYPE_SPEECH else C.AUDIO_CONTENT_TYPE_MOVIE,
            handleAudioFocus = !isTV && !isProblematicDevice,
            handleAudioBecomingNoisy = !isTV && !isProblematicDevice,
            forceCompatibilityMode = isProblematicDevice
        )
    }
    
    fun configureSystemAudio(context: Context, forceCompatibility: Boolean = false) {
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            
            if (forceCompatibility || isDeviceProblematic()) {
                // Configuración para dispositivos problemáticos
                audioManager.mode = AudioManager.MODE_NORMAL
                
                // Asegurar volumen mínimo
                val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                
                if (currentVolume < maxVolume * 0.5) {
                    audioManager.setStreamVolume(
                        AudioManager.STREAM_MUSIC, 
                        (maxVolume * 0.7).toInt(), 
                        0
                    )
                }
                
                println("IPTV: Audio configurado para dispositivo problemático")
            } else {
                // Configuración estándar
                audioManager.mode = AudioManager.MODE_NORMAL
                println("IPTV: Audio configurado modo estándar")
            }
        } catch (e: Exception) {
            println("IPTV: Error configurando audio del sistema: ${e.message}")
        }
    }
    
    fun analyzeAudioFormat(format: Format): AudioAnalysis {
        val codec = format.codecs ?: "unknown"
        val sampleRate = format.sampleRate
        val channels = format.channelCount
        
        val isProblematic = isCodecProblematic(codec)
        val isCompatible = isCodecCompatible(codec)
        val needsTranscoding = isProblematic && isDeviceProblematic()
        
        val recommendations = mutableListOf<String>()
        
        if (isProblematic) {
            recommendations.add("Códec problemático detectado: $codec")
        }
        
        if (channels > 2 && isDeviceProblematic()) {
            recommendations.add("Canales múltiples ($channels) pueden causar problemas")
        }
        
        if (sampleRate > 48000) {
            recommendations.add("Sample rate alto ($sampleRate Hz) puede causar problemas")
        }
        
        if (needsTranscoding) {
            recommendations.add("Se recomienda transcodificación")
        }
        
        return AudioAnalysis(
            codec = codec,
            sampleRate = sampleRate,
            channels = channels,
            isProblematic = isProblematic,
            isCompatible = isCompatible,
            needsTranscoding = needsTranscoding,
            recommendations = recommendations
        )
    }
    
    data class AudioConfiguration(
        val usage: Int,
        val contentType: Int,
        val handleAudioFocus: Boolean,
        val handleAudioBecomingNoisy: Boolean,
        val forceCompatibilityMode: Boolean
    )
    
    data class AudioAnalysis(
        val codec: String,
        val sampleRate: Int,
        val channels: Int,
        val isProblematic: Boolean,
        val isCompatible: Boolean,
        val needsTranscoding: Boolean,
        val recommendations: List<String>
    )
}