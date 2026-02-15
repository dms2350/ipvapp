package com.dms2350.iptvapp.data.repository

import com.dms2350.iptvapp.data.api.IPTVApi
import com.dms2350.iptvapp.data.api.dto.toDomain
import com.dms2350.iptvapp.data.database.dao.ChannelDao
import com.dms2350.iptvapp.data.database.entities.toDomain
import com.dms2350.iptvapp.data.database.entities.toEntity
import com.dms2350.iptvapp.domain.model.Channel
import com.dms2350.iptvapp.domain.repository.ChannelRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber
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
            Timber.d("IPTV: Iniciando carga desde API...")
            
            // Verificar canales existentes antes de limpiar
            val existingCount = channelDao.getChannelCount()
            Timber.d("IPTV: Canales existentes en BD antes de limpiar: $existingCount")
            
            // Limpiar datos locales primero
            channelDao.deleteAllChannels()
            Timber.d("IPTV: BD limpiada")
            
            val startTime = System.currentTimeMillis()
            val response = kotlinx.coroutines.withTimeout(15000) { // 15 segundos timeout
                api.getChannels()
            }
            val endTime = System.currentTimeMillis()
            
            Timber.d("IPTV: API respondió en ${endTime - startTime}ms")
            Timber.d("IPTV: Response code: ${response.code()}")
            
            if (response.isSuccessful) {
                response.body()?.let { channelDtos ->
                    Timber.d("IPTV: Canales recibidos: ${channelDtos.size}")
                    val channels = channelDtos.map { it.toDomain().toEntity() }
                    channelDao.insertChannels(channels)
                    
                    val finalCount = channelDao.getChannelCount()
                    Timber.d("IPTV: Canales guardados en BD: $finalCount")
                } ?: Timber.d("IPTV: Response body es null")
            } else {
                Timber.d("IPTV: Error en API: ${response.code()} - ${response.message()}")
            }
        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
            Timber.d("IPTV: TIMEOUT - API no responde en 15 segundos")
        } catch (e: Exception) {
            Timber.d("IPTV: Exception: ${e.message}")
            e.printStackTrace()
        }
    }


}

