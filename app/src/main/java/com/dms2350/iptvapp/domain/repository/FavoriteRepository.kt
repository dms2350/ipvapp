package com.dms2350.iptvapp.domain.repository

import com.dms2350.iptvapp.domain.model.Channel
import kotlinx.coroutines.flow.Flow

interface FavoriteRepository {
    fun getFavoriteChannels(): Flow<List<Channel>>
    suspend fun isFavorite(channelId: Int): Boolean
    suspend fun addFavorite(channelId: Int)
    suspend fun removeFavorite(channelId: Int)
}
