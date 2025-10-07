package com.dms2350.iptvapp.di

import android.content.Context
import androidx.room.Room
import com.dms2350.iptvapp.data.database.IPTVDatabase
import com.dms2350.iptvapp.data.database.dao.ChannelDao
import com.dms2350.iptvapp.data.database.dao.CategoryDao
import com.dms2350.iptvapp.data.database.dao.FavoriteDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideIPTVDatabase(@ApplicationContext context: Context): IPTVDatabase {
        return Room.databaseBuilder(
            context,
            IPTVDatabase::class.java,
            IPTVDatabase.DATABASE_NAME
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    fun provideChannelDao(database: IPTVDatabase): ChannelDao {
        return database.channelDao()
    }

    @Provides
    fun provideCategoryDao(database: IPTVDatabase): CategoryDao {
        return database.categoryDao()
    }

    @Provides
    fun provideFavoriteDao(database: IPTVDatabase): FavoriteDao {
        return database.favoriteDao()
    }
}
