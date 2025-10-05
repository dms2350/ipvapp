package com.iptv.data.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context
import com.iptv.data.database.dao.ChannelDao
import com.iptv.data.database.dao.CategoryDao
import com.iptv.data.database.dao.FavoriteDao
import com.iptv.data.database.entities.ChannelEntity
import com.iptv.data.database.entities.CategoryEntity
import com.iptv.data.database.entities.FavoriteEntity

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