package com.dms2350.iptvapp.data.repository

import com.dms2350.iptvapp.data.api.IPTVApi
import com.dms2350.iptvapp.data.api.dto.toDomain
import com.dms2350.iptvapp.data.database.IPTVDatabase
import com.dms2350.iptvapp.data.database.entities.CategoryEntity
import com.dms2350.iptvapp.data.database.entities.toEntity
import com.dms2350.iptvapp.data.database.entities.toDomain
import com.dms2350.iptvapp.domain.model.Category
import com.dms2350.iptvapp.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryRepositoryImpl @Inject constructor(
    private val api: IPTVApi,
    private val database: IPTVDatabase
) : CategoryRepository {

    override fun getAllCategories(): Flow<List<Category>> {
        return database.categoryDao().getAllCategories().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getCategoryById(id: Int): Category? {
        return database.categoryDao().getCategoryById(id)?.toDomain()
    }

    override suspend fun refreshCategories() {
        try {
            val response = api.getCategories()
            if (response.isSuccessful) {
                val categories = response.body()?.map { it.toDomain() } ?: emptyList()
                val entities = categories.map { it.toEntity() }
                database.categoryDao().insertCategories(entities)
            }
        } catch (e: Exception) {
            println("Error refreshing categories: ${e.message}")
        }
    }
}