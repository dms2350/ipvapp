package com.dms2350.iptvapp.data.repository

import com.dms2350.iptvapp.data.database.dao.ChannelDao
import com.dms2350.iptvapp.data.database.dao.FavoriteDao
import com.dms2350.iptvapp.data.database.entities.FavoriteEntity
import com.dms2350.iptvapp.data.database.entities.toDomain
import com.dms2350.iptvapp.domain.model.Channel
import com.dms2350.iptvapp.domain.repository.FavoriteRepository
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
