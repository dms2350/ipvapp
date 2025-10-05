package com.iptv.presentation.ui.channels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iptv.domain.model.Channel
import com.iptv.domain.usecase.GetChannelsUseCase
import com.iptv.domain.repository.ChannelRepository
import com.iptv.domain.repository.FavoriteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChannelsViewModel @Inject constructor(
    private val getChannelsUseCase: GetChannelsUseCase,
    private val channelRepository: ChannelRepository,
    private val favoriteRepository: FavoriteRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChannelsUiState())
    val uiState: StateFlow<ChannelsUiState> = _uiState.asStateFlow()

    init {
        loadChannels()
        refreshChannels()
        // Auto-refresh deshabilitado para evitar interrupciones
    }

    private fun loadChannels() {
        viewModelScope.launch {
            println("IPTV: === CARGANDO CANALES DESDE BD LOCAL ===")
            getChannelsUseCase().collect { channels ->
                println("IPTV: Canales cargados desde BD: ${channels.size}")
                _uiState.value = _uiState.value.copy(
                    channels = channels,
                    isLoading = false
                )
                
                if (channels.isEmpty()) {
                    println("IPTV: WARNING: No hay canales en BD local")
                } else {
                    println("IPTV: OK: ${channels.size} canales disponibles")
                    channels.take(3).forEach { channel ->
                        println("IPTV: Canal: ${channel.name} - ${channel.streamUrl}")
                    }
                }
            }
        }
    }

    fun refreshChannels() {
        viewModelScope.launch {
            println("IPTV: === INICIANDO REFRESH CHANNELS ===")
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                println("IPTV: Llamando channelRepository.refreshChannels()")
                channelRepository.refreshChannels()
                println("IPTV: refreshChannels() completado")
            } catch (e: Exception) {
                println("IPTV: Error en refreshChannels(): ${e.message}")
                e.printStackTrace()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun toggleFavorite(channelId: Int) {
        viewModelScope.launch {
            val isFavorite = favoriteRepository.isFavorite(channelId)
            if (isFavorite) {
                favoriteRepository.removeFavorite(channelId)
            } else {
                favoriteRepository.addFavorite(channelId)
            }
        }
    }

    suspend fun isFavorite(channelId: Int): Boolean {
        return favoriteRepository.isFavorite(channelId)
    }
}

data class ChannelsUiState(
    val channels: List<Channel> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)