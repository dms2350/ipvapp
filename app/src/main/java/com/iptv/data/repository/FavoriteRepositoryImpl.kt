package com.iptv.data.repository

import com.iptv.data.database.dao.ChannelDao
import com.iptv.data.database.dao.FavoriteDao
import com.iptv.data.database.entities.FavoriteEntity
import com.iptv.data.database.entities.toDomain
import com.iptv.domain.model.Channel
import com.iptv.domain.repository.FavoriteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FavoriteRepositoryImpl @Inject constructor(
    private val favoriteDao: FavoriteDao,
    private val channelDao: ChannelDao
) : FavoriteRepository {

    override fun getFavoriteChannels(): Flow<List<Channel>> {
        return combine(
            favoriteDao.getAllFavorites(),
            channelDao.getAllChannels()
        ) { favorites, channels ->
            val favoriteIds = favorites.map { it.channelId }.toSet()
            channels.filter { it.id in favoriteIds }.map { it.toDomain() }
        }
    }

    override suspend fun isFavorite(channelId: Int): Boolean {
        return favoriteDao.isFavorite(channelId)
    }

    override suspend fun addFavorite(channelId: Int) {
        favoriteDao.addFavorite(FavoriteEntity(channelId))
    }

    override suspend fun removeFavorite(channelId: Int) {
        favoriteDao.removeFavorite(channelId)
    }
}