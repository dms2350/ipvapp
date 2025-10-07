package com.dms2350.iptvapp.domain.model

data class Category(
    val id: Int,
    val name: String,
    val description: String?,
    val iconUrl: String?,
    val sortOrder: Int = 0,
    val isActive: Boolean = true
)
