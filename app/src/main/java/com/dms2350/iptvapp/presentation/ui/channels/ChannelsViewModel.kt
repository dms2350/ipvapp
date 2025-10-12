package com.dms2350.iptvapp.presentation.ui.channels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dms2350.iptvapp.domain.model.Channel
import com.dms2350.iptvapp.domain.usecase.GetChannelsUseCase
import com.dms2350.iptvapp.domain.repository.ChannelRepository
import com.dms2350.iptvapp.domain.repository.CategoryRepository
import com.dms2350.iptvapp.domain.repository.FavoriteRepository
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
    private val favoriteRepository: FavoriteRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChannelsUiState())
    val uiState: StateFlow<ChannelsUiState> = _uiState.asStateFlow()

    init {
        loadChannels()
        loadCategories()
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
                    
                    // Mostrar agrupación por categorías
                    val groupedChannels = getChannelsGroupedByCategory()
                    println("IPTV: === CANALES AGRUPADOS POR CATEGORÍA ===")
                    groupedChannels.forEach { (categoryName, categoryChannels) ->
                        println("IPTV: Categoría '$categoryName': ${categoryChannels.size} canales")
                        categoryChannels.take(2).forEach { channel ->
                            println("IPTV:   - ${channel.name}")
                        }
                    }
                    println("IPTV: ========================================")
                }
            }
        }
    }

    private fun loadCategories() {
        viewModelScope.launch {
            println("IPTV: === CARGANDO CATEGORIAS ===")
            try {
                categoryRepository.getAllCategories().collect { categories ->
                    val categoryMap = categories.associate { it.id to it.name }
                    val categoryOrder = categories.associate { it.id to it.sortOrder }
                    println("IPTV: Categorias cargadas: ${categoryMap.size}")
                    _uiState.value = _uiState.value.copy(
                        categories = categoryMap,
                        categoryOrder = categoryOrder
                    )
                }
            } catch (e: Exception) {
                println("IPTV: Error cargando categorias: ${e.message}")
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
                categoryRepository.refreshCategories()
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
    
    fun getChannelsGroupedByCategory(): Map<String, List<Channel>> {
        val channels = _uiState.value.channels
        val categories = _uiState.value.categories
        val categoryOrder = _uiState.value.categoryOrder
        
        // Agrupar canales por category_id
        val groupedChannels = channels.groupBy { channel ->
            val categoryName = categories[channel.categoryId] ?: "Sin categoría"
            categoryName
        }
        
        // Ordenar las categorías según sort_order del backend
        return groupedChannels.toSortedMap { category1, category2 ->
            val categoryId1 = categories.entries.find { it.value == category1 }?.key
            val categoryId2 = categories.entries.find { it.value == category2 }?.key
            
            val order1 = categoryOrder[categoryId1] ?: Int.MAX_VALUE
            val order2 = categoryOrder[categoryId2] ?: Int.MAX_VALUE
            
            order1.compareTo(order2)
        }
    }
    
    fun getChannelsByCategory(categoryName: String): List<Channel> {
        val channels = _uiState.value.channels
        val categories = _uiState.value.categories
        
        val categoryId = categories.entries.find { it.value == categoryName }?.key
        
        return channels.filter { channel ->
            channel.categoryId == categoryId
        }.sortedBy { it.name }
    }
    
    fun getCategoryNames(): List<String> {
        val categories = _uiState.value.categories
        val categoryOrder = _uiState.value.categoryOrder
        
        return categories.entries
            .sortedBy { categoryOrder[it.key] ?: Int.MAX_VALUE }
            .map { it.value }
    }
    
    fun getCategoryInfo(): List<CategoryInfo> {
        val channels = _uiState.value.channels
        val categories = _uiState.value.categories
        val categoryOrder = _uiState.value.categoryOrder
        
        return categories.entries
            .sortedBy { categoryOrder[it.key] ?: Int.MAX_VALUE }
            .map { (categoryId, categoryName) ->
                val channelCount = channels.count { it.categoryId == categoryId }
                CategoryInfo(
                    id = categoryId,
                    name = categoryName,
                    channelCount = channelCount,
                    sortOrder = categoryOrder[categoryId] ?: Int.MAX_VALUE
                )
            }
    }
    
    data class CategoryInfo(
        val id: Int,
        val name: String,
        val channelCount: Int,
        val sortOrder: Int
    )
}

data class ChannelsUiState(
    val channels: List<Channel> = emptyList(),
    val categories: Map<Int, String> = emptyMap(),
    val categoryOrder: Map<Int, Int> = emptyMap(),
    val isLoading: Boolean = true,
    val error: String? = null
)
