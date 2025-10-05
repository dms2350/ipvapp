package com.iptv.presentation.ui.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iptv.domain.model.Channel
import com.iptv.domain.repository.ChannelRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val vlcPlayerManager: VLCPlayerManager,
    private val channelRepository: ChannelRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()
    
    val mediaPlayer get() = vlcPlayerManager.mediaPlayer
    
    private var allChannels: List<Channel> = emptyList()
    private var currentChannelIndex = 0
    
    init {
        loadChannels()
        // Configurar auto-skip solo para errores reales, no para navegación manual
        vlcPlayerManager.setOnChannelErrorListener {
            // Solo auto-skip si no es navegación manual desde menú
            if (_uiState.value.isPlaying) {
                println("IPTV: Canal no funciona - saltando al siguiente")
                nextChannel()
            } else {
                println("IPTV: Error en canal pero no auto-skip (navegación manual)")
            }
        }
    }
    
    private fun loadChannels() {
        viewModelScope.launch {
            try {
                allChannels = channelRepository.getAllChannels().first()
                println("IPTV: Canales cargados: ${allChannels.size}")
            } catch (e: Exception) {
                println("IPTV: Error cargando canales: ${e.message}")
            }
        }
    }

    fun playChannel(channel: Channel) {
        viewModelScope.launch {
            println("IPTV: Iniciando reproducción de: ${channel.name} - URL: ${channel.streamUrl}")
            
            _uiState.value = _uiState.value.copy(
                currentChannel = channel,
                isLoading = true,
                error = null,
                showChannelInfo = true,
                showControls = true
            )
            
            try {
                // Detener reproducción anterior antes de iniciar nueva
                vlcPlayerManager.stop()
                
                vlcPlayerManager.playStream(channel.streamUrl)
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isPlaying = true
                )
                
                // Ocultar información y controles después de 3 segundos
                kotlinx.coroutines.delay(3000)
                _uiState.value = _uiState.value.copy(
                    showChannelInfo = false,
                    showControls = false
                )
                
                println("IPTV: Reproducción iniciada exitosamente")
            } catch (e: Exception) {
                println("IPTV: Error en playChannel: ${e.message}")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isPlaying = false,
                    error = "Error al reproducir: ${e.message}"
                )
            }
        }
    }

    fun pauseResume() {
        if (_uiState.value.isPlaying) {
            vlcPlayerManager.pause()
            _uiState.value = _uiState.value.copy(isPlaying = false)
        } else {
            vlcPlayerManager.resume()
            _uiState.value = _uiState.value.copy(isPlaying = true)
        }
    }

    fun stop() {
        vlcPlayerManager.stop()
        _uiState.value = _uiState.value.copy(
            isPlaying = false,
            currentChannel = null
        )
    }
    
    fun setVolume(volume: Int) {
        vlcPlayerManager.setVolume(volume)
    }
    
    fun getVolume(): Int {
        return vlcPlayerManager.getVolume()
    }
    
    fun nextChannel() {
        viewModelScope.launch {
            if (allChannels.isEmpty()) {
                allChannels = channelRepository.getAllChannels().first()
            }
            if (allChannels.isNotEmpty()) {
                val newIndex = (currentChannelIndex + 1) % allChannels.size
                println("IPTV: Siguiente canal - Índice: $currentChannelIndex -> $newIndex")
                currentChannelIndex = newIndex
                val nextChannel = allChannels[currentChannelIndex]
                println("IPTV: Reproduciendo: ${nextChannel.name}")
                playChannel(nextChannel)
            }
        }
    }
    
    fun previousChannel() {
        viewModelScope.launch {
            if (allChannels.isEmpty()) {
                allChannels = channelRepository.getAllChannels().first()
            }
            if (allChannels.isNotEmpty()) {
                val newIndex = if (currentChannelIndex > 0) {
                    currentChannelIndex - 1
                } else {
                    allChannels.size - 1
                }
                println("IPTV: Canal anterior - Índice: $currentChannelIndex -> $newIndex")
                currentChannelIndex = newIndex
                val prevChannel = allChannels[currentChannelIndex]
                println("IPTV: Reproduciendo: ${prevChannel.name}")
                playChannel(prevChannel)
            }
        }
    }
    
    fun setCurrentChannel(channel: Channel) {
        viewModelScope.launch {
            if (allChannels.isEmpty()) {
                allChannels = channelRepository.getAllChannels().first()
            }
            val foundIndex = allChannels.indexOfFirst { it.id == channel.id }
            currentChannelIndex = if (foundIndex != -1) foundIndex else 0
            println("IPTV: Canal actual establecido - Índice: $currentChannelIndex, Total: ${allChannels.size}")
        }
    }
    
    fun forceReconnect() {
        vlcPlayerManager.forceReconnect()
    }
    
    fun toggleChannelInfo() {
        _uiState.value = _uiState.value.copy(
            showChannelInfo = !_uiState.value.showChannelInfo,
            showControls = !_uiState.value.showControls
        )
    }
    

    




    override fun onCleared() {
        super.onCleared()
        vlcPlayerManager.release()
    }
}

data class PlayerUiState(
    val currentChannel: Channel? = null,
    val isPlaying: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val showChannelInfo: Boolean = false,
    val showControls: Boolean = false
)