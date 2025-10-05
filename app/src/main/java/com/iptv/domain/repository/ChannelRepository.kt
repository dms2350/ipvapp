package com.iptv.domain.repository

import com.iptv.domain.model.Channel
import kotlinx.coroutines.flow.Flow

interface ChannelRepository {
    fun getAllChannels(): Flow<List<Channel>>
    fun getChannelsByCategory(categoryId: Int): Flow<List<Channel>>
    suspend fun getChannelById(id: Int): Channel?
    suspend fun refreshChannels()
}