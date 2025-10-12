package com.dms2350.iptvapp.presentation.ui.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dms2350.iptvapp.domain.model.Channel
import com.dms2350.iptvapp.domain.repository.ChannelRepository
import com.dms2350.iptvapp.domain.repository.CategoryRepository
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
    private val channelRepository: ChannelRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()
    
    val mediaPlayer get() = vlcPlayerManager.mediaPlayer
    
    private var allChannels: List<Channel> = emptyList()
    private var currentChannelIndex = 0
    private var categories: Map<Int, String> = emptyMap()
    private var channelsByCategory: Map<String, List<Channel>> = emptyMap()
    private var currentCategoryIndex = 0
    private var currentChannelInCategoryIndex = 0
    
    init {
        loadChannels()
        loadCategories()
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
                updateChannelsByCategory()
                println("IPTV: Canales cargados: ${allChannels.size}")
            } catch (e: Exception) {
                println("IPTV: Error cargando canales: ${e.message}")
            }
        }
    }
    
    private fun loadCategories() {
        viewModelScope.launch {
            try {
                categoryRepository.getAllCategories().collect { categoriesList ->
                    categories = categoriesList.associate { it.id to it.name }
                    updateChannelsByCategory()
                    println("IPTV: Categorías cargadas: ${categories.size}")
                }
            } catch (e: Exception) {
                println("IPTV: Error cargando categorías: ${e.message}")
            }
        }
    }
    
    private fun updateChannelsByCategory() {
        channelsByCategory = allChannels.groupBy { channel ->
            categories[channel.categoryId] ?: "Sin categoría"
        }
    }

    fun playChannel(channel: Channel) {
        viewModelScope.launch {
            println("IPTV: Iniciando reproducción de: ${channel.name} - URL: ${channel.streamUrl}")
            
            // Asegurar que los datos estén cargados antes de mostrar la información
            ensureDataLoaded()
            
            val categoryName = getCategoryNameForChannel(channel)
            val channelNumber = getChannelNumberForChannel(channel)
            
            _uiState.value = _uiState.value.copy(
                currentChannel = channel,
                isLoading = true,
                error = null,
                showChannelInfo = true,
                showControls = true,
                categoryName = categoryName,
                channelNumber = channelNumber
            )
            
            try {
                // Detener reproducción anterior antes de iniciar nueva
                vlcPlayerManager.stop()
                
                vlcPlayerManager.playStream(channel.streamUrl)
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isPlaying = true,
                    categoryName = categoryName,
                    channelNumber = channelNumber
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
            if (channelsByCategory.isEmpty()) return@launch
            
            val categoryNames = channelsByCategory.keys.toList()
            if (currentCategoryIndex >= categoryNames.size) return@launch
            
            val currentCategoryName = categoryNames[currentCategoryIndex]
            val channelsInCategory = channelsByCategory[currentCategoryName] ?: return@launch
            
            if (channelsInCategory.isNotEmpty()) {
                // Si estamos en el último canal de la categoría actual
                if (currentChannelInCategoryIndex >= channelsInCategory.size - 1) {
                    // Pasar a la siguiente categoría
                    currentCategoryIndex = (currentCategoryIndex + 1) % categoryNames.size
                    currentChannelInCategoryIndex = 0
                    
                    val newCategoryName = categoryNames[currentCategoryIndex]
                    val newChannelsInCategory = channelsByCategory[newCategoryName] ?: return@launch
                    
                    if (newChannelsInCategory.isNotEmpty()) {
                        val nextChannel = newChannelsInCategory[0]
                        currentChannelIndex = allChannels.indexOfFirst { it.id == nextChannel.id }
                        println("IPTV: Pasando a siguiente categoría '$newCategoryName' - Canal 1/${newChannelsInCategory.size}")
                        playChannel(nextChannel)
                    }
                } else {
                    // Siguiente canal en la misma categoría
                    currentChannelInCategoryIndex++
                    val nextChannel = channelsInCategory[currentChannelInCategoryIndex]
                    currentChannelIndex = allChannels.indexOfFirst { it.id == nextChannel.id }
                    println("IPTV: Siguiente canal en categoría '$currentCategoryName' - ${currentChannelInCategoryIndex + 1}/${channelsInCategory.size}")
                    playChannel(nextChannel)
                }
            }
        }
    }
    
    fun previousChannel() {
        viewModelScope.launch {
            if (channelsByCategory.isEmpty()) return@launch
            
            val categoryNames = channelsByCategory.keys.toList()
            if (currentCategoryIndex >= categoryNames.size) return@launch
            
            val currentCategoryName = categoryNames[currentCategoryIndex]
            val channelsInCategory = channelsByCategory[currentCategoryName] ?: return@launch
            
            if (channelsInCategory.isNotEmpty()) {
                // Si estamos en el primer canal de la categoría actual
                if (currentChannelInCategoryIndex <= 0) {
                    // Pasar a la categoría anterior
                    currentCategoryIndex = if (currentCategoryIndex > 0) {
                        currentCategoryIndex - 1
                    } else {
                        categoryNames.size - 1
                    }
                    
                    val newCategoryName = categoryNames[currentCategoryIndex]
                    val newChannelsInCategory = channelsByCategory[newCategoryName] ?: return@launch
                    
                    if (newChannelsInCategory.isNotEmpty()) {
                        // Ir al último canal de la categoría anterior
                        currentChannelInCategoryIndex = newChannelsInCategory.size - 1
                        val prevChannel = newChannelsInCategory[currentChannelInCategoryIndex]
                        currentChannelIndex = allChannels.indexOfFirst { it.id == prevChannel.id }
                        println("IPTV: Pasando a categoría anterior '$newCategoryName' - Canal ${currentChannelInCategoryIndex + 1}/${newChannelsInCategory.size}")
                        playChannel(prevChannel)
                    }
                } else {
                    // Canal anterior en la misma categoría
                    currentChannelInCategoryIndex--
                    val prevChannel = channelsInCategory[currentChannelInCategoryIndex]
                    currentChannelIndex = allChannels.indexOfFirst { it.id == prevChannel.id }
                    println("IPTV: Canal anterior en categoría '$currentCategoryName' - ${currentChannelInCategoryIndex + 1}/${channelsInCategory.size}")
                    playChannel(prevChannel)
                }
            }
        }
    }
    
    fun setCurrentChannel(channel: Channel) {
        viewModelScope.launch {
            if (allChannels.isEmpty()) {
                allChannels = channelRepository.getAllChannels().first()
            }
            if (categories.isEmpty()) {
                val categoriesList = categoryRepository.getAllCategories().first()
                categories = categoriesList.associate { it.id to it.name }
            }
            updateChannelsByCategory()
            
            val foundIndex = allChannels.indexOfFirst { it.id == channel.id }
            currentChannelIndex = if (foundIndex != -1) foundIndex else 0
            
            // Actualizar índices de categoría
            updateCategoryIndices(channel)
            
            println("IPTV: Canal actual establecido - Índice: $currentChannelIndex, Total: ${allChannels.size}")
            println("IPTV: Categoría: ${getCurrentCategoryName()}, Número: ${getCurrentChannelNumber()}")
        }
    }
    
    private fun updateCategoryIndices(channel: Channel) {
        val categoryName = categories[channel.categoryId] ?: "Sin categoría"
        
        // Si channelsByCategory está vacío, asegurar que se actualice
        if (channelsByCategory.isEmpty()) {
            updateChannelsByCategory()
        }
        
        val categoryNames = channelsByCategory.keys.toList()
        currentCategoryIndex = categoryNames.indexOf(categoryName).takeIf { it >= 0 } ?: 0
        
        val channelsInCategory = channelsByCategory[categoryName] ?: emptyList()
        currentChannelInCategoryIndex = channelsInCategory.indexOfFirst { it.id == channel.id }.takeIf { it >= 0 } ?: 0
        
        println("IPTV: Índices actualizados - Categoría: $currentCategoryIndex ($categoryName), Canal: $currentChannelInCategoryIndex")
    }
    
    fun nextCategory() {
        viewModelScope.launch {
            if (channelsByCategory.isEmpty()) return@launch
            
            val categoryNames = channelsByCategory.keys.toList()
            currentCategoryIndex = (currentCategoryIndex + 1) % categoryNames.size
            currentChannelInCategoryIndex = 0
            
            val newCategoryName = categoryNames[currentCategoryIndex]
            val channelsInCategory = channelsByCategory[newCategoryName] ?: return@launch
            
            if (channelsInCategory.isNotEmpty()) {
                val newChannel = channelsInCategory[0]
                currentChannelIndex = allChannels.indexOfFirst { it.id == newChannel.id }
                playChannel(newChannel)
            }
        }
    }
    
    fun previousCategory() {
        viewModelScope.launch {
            if (channelsByCategory.isEmpty()) return@launch
            
            val categoryNames = channelsByCategory.keys.toList()
            currentCategoryIndex = if (currentCategoryIndex > 0) {
                currentCategoryIndex - 1
            } else {
                categoryNames.size - 1
            }
            currentChannelInCategoryIndex = 0
            
            val newCategoryName = categoryNames[currentCategoryIndex]
            val channelsInCategory = channelsByCategory[newCategoryName] ?: return@launch
            
            if (channelsInCategory.isNotEmpty()) {
                val newChannel = channelsInCategory[0]
                currentChannelIndex = allChannels.indexOfFirst { it.id == newChannel.id }
                playChannel(newChannel)
            }
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
    
    fun getCurrentCategoryName(): String {
        val currentChannel = _uiState.value.currentChannel ?: return "Sin categoría"
        return categories[currentChannel.categoryId] ?: "Sin categoría"
    }
    
    fun getCurrentChannelNumber(): String {
        val currentChannel = _uiState.value.currentChannel ?: return "1/1"
        return getChannelNumberForChannel(currentChannel)
    }
    
    private suspend fun ensureDataLoaded() {
        if (allChannels.isEmpty()) {
            allChannels = channelRepository.getAllChannels().first()
        }
        if (categories.isEmpty()) {
            val categoriesList = categoryRepository.getAllCategories().first()
            categories = categoriesList.associate { it.id to it.name }
        }
        updateChannelsByCategory()
    }
    
    private fun getCategoryNameForChannel(channel: Channel): String {
        return categories[channel.categoryId] ?: "Sin categoría"
    }
    
    private fun getChannelNumberForChannel(channel: Channel): String {
        val categoryName = getCategoryNameForChannel(channel)
        val channelsInCategory = allChannels.filter { 
            getCategoryNameForChannel(it) == categoryName 
        }.sortedBy { it.name }
        
        val channelIndex = channelsInCategory.indexOfFirst { it.id == channel.id }
        val position = if (channelIndex >= 0) channelIndex + 1 else 1
        val total = channelsInCategory.size
        
        println("IPTV: Canal ${channel.name} en categoría '$categoryName': $position/$total")
        return "$position/$total"
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
    val showControls: Boolean = false,
    val categoryName: String = "",
    val channelNumber: String = "1/1"
)
