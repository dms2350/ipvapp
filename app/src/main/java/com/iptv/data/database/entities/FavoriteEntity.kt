package com.iptv.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val channelId: Int,
    val addedAt: Long = System.currentTimeMillis()
)