package com.dms2350.iptvapp.di

import com.dms2350.iptvapp.data.repository.ChannelRepositoryImpl
import com.dms2350.iptvapp.data.repository.CategoryRepositoryImpl
import com.dms2350.iptvapp.data.repository.FavoriteRepositoryImpl
import com.dms2350.iptvapp.data.repository.NotificationRepositoryImpl
import com.dms2350.iptvapp.domain.repository.ChannelRepository
import com.dms2350.iptvapp.domain.repository.CategoryRepository
import com.dms2350.iptvapp.domain.repository.FavoriteRepository
import com.dms2350.iptvapp.domain.repository.NotificationRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindChannelRepository(
        channelRepositoryImpl: ChannelRepositoryImpl
    ): ChannelRepository

    @Binds
    @Singleton
    abstract fun bindFavoriteRepository(
        favoriteRepositoryImpl: FavoriteRepositoryImpl
    ): FavoriteRepository

    @Binds
    @Singleton
    abstract fun bindCategoryRepository(
        categoryRepositoryImpl: CategoryRepositoryImpl
    ): CategoryRepository

    @Binds
    @Singleton
    abstract fun bindNotificationRepository(
        notificationRepositoryImpl: NotificationRepositoryImpl
    ): NotificationRepository
}
