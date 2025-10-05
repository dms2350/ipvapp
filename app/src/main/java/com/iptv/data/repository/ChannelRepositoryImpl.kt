package com.iptv.data.repository

import com.iptv.data.api.IPTVApi
import com.iptv.data.api.dto.toDomain
import com.iptv.data.database.dao.ChannelDao
import com.iptv.data.database.entities.toDomain
import com.iptv.data.database.entities.toEntity
import com.iptv.domain.model.Channel
import com.iptv.domain.repository.ChannelRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChannelRepositoryImpl @Inject constructor(
    private val api: IPTVApi,
    private val channelDao: ChannelDao
) : ChannelRepository {

    override fun getAllChannels(): Flow<List<Channel>> {
        return channelDao.getAllChannels().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getChannelsByCategory(categoryId: Int): Flow<List<Channel>> {
        return channelDao.getChannelsByCategory(categoryId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getChannelById(id: Int): Channel? {
        return channelDao.getChannelById(id)?.toDomain()
    }

    override suspend fun refreshChannels() {
        try {
            println("IPTV: Iniciando carga desde API...")
            
            // Verificar canales existentes antes de limpiar
            val existingCount = channelDao.getChannelCount()
            println("IPTV: Canales existentes en BD antes de limpiar: $existingCount")
            
            // Limpiar datos locales primero
            channelDao.deleteAllChannels()
            println("IPTV: BD limpiada")
            
            val response = api.getChannels()
            println("IPTV: Response code: ${response.code()}")
            
            if (response.isSuccessful) {
                response.body()?.let { channelDtos ->
                    println("IPTV: Canales recibidos: ${channelDtos.size}")
                    val channels = channelDtos.map { it.toDomain().toEntity() }
                    channelDao.insertChannels(channels)
                    
                    val finalCount = channelDao.getChannelCount()
                    println("IPTV: Canales guardados en BD: $finalCount")
                } ?: println("IPTV: Response body es null")
            } else {
                println("IPTV: Error en API: ${response.code()} - ${response.message()}")
            }
        } catch (e: Exception) {
            println("IPTV: Exception: ${e.message}")
            e.printStackTrace()
        }
    }


}