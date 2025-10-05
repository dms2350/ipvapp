package com.iptv.presentation.ui.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iptv.domain.model.Channel
import com.iptv.domain.repository.FavoriteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val favoriteRepository: FavoriteRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FavoritesUiState())
    val uiState: StateFlow<FavoritesUiState> = _uiState.asStateFlow()

    init {
        loadFavorites()
    }

    private fun loadFavorites() {
        viewModelScope.launch {
            favoriteRepository.getFavoriteChannels().collect { channels ->
                _uiState.value = _uiState.value.copy(
                    favoriteChannels = channels,
                    isLoading = false
                )
            }
        }
    }

    fun removeFavorite(channelId: Int) {
        viewModelScope.launch {
            favoriteRepository.removeFavorite(channelId)
        }
    }
}

data class FavoritesUiState(
    val favoriteChannels: List<Channel> = emptyList(),
    val isLoading: Boolean = true
)