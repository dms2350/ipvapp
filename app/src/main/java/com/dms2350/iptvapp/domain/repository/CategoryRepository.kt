package com.dms2350.iptvapp.domain.repository

import com.dms2350.iptvapp.domain.model.Category
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {
    fun getAllCategories(): Flow<List<Category>>
    suspend fun getCategoryById(id: Int): Category?
    suspend fun refreshCategories()
}
