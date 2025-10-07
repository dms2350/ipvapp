package com.dms2350.iptvapp.data.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context
import com.dms2350.iptvapp.data.database.dao.ChannelDao
import com.dms2350.iptvapp.data.database.dao.CategoryDao
import com.dms2350.iptvapp.data.database.dao.FavoriteDao
import com.dms2350.iptvapp.data.database.entities.ChannelEntity
import com.dms2350.iptvapp.data.database.entities.CategoryEntity
import com.dms2350.iptvapp.data.database.entities.FavoriteEntity

@Database(
    entities = [ChannelEntity::class, CategoryEntity::class, FavoriteEntity::class],
    version = 3,
    exportSchema = false
)
abstract class IPTVDatabase : RoomDatabase() {
    abstract fun channelDao(): ChannelDao
    abstract fun categoryDao(): CategoryDao
    abstract fun favoriteDao(): FavoriteDao

    companion object {
        const val DATABASE_NAME = "iptv_database"
    }
}
