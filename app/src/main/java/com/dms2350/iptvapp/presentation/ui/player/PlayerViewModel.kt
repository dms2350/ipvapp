package com.dms2350.iptvapp.presentation.ui.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dms2350.iptvapp.domain.model.Channel
import com.dms2350.iptvapp.domain.repository.ChannelRepository
import com.dms2350.iptvapp.domain.repository.CategoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val vlcPlayerManager: VLCPlayerManager,
    private val channelRepository: ChannelRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()
    
    val mediaPlayer get() = vlcPlayerManager.mediaPlayer
    
    private var allChannels: List<Channel> = emptyList()
    private var currentChannelIndex = 0
    private var categories: Map<Int, String> = emptyMap()
    private var channelsByCategory: Map<String, List<Channel>> = emptyMap()
    private var currentCategoryIndex = 0
    private var currentChannelInCategoryIndex = 0
    
    // Variables para el monitor de música
    private var musicMonitorJob: Job? = null
    private var lastPosition: Long = 0
    private var positionCheckCount = 0
    private var bufferHealthCheckCount = 0
    
    // Variables para el monitor de video (todos los canales)
    private var videoMonitorJob: Job? = null
    private var lastAudioDelay: Long = 0
    
    init {
        loadChannels()
        loadCategories()
        // Configurar auto-skip solo para errores reales, no para navegación manual
        vlcPlayerManager.setOnChannelErrorListener {
            // Solo auto-skip si no es navegación manual desde menú
            if (_uiState.value.isPlaying) {
                println("IPTV: Canal no funciona - saltando al siguiente")
                nextChannel()
            } else {
                println("IPTV: Error en canal pero no auto-skip (navegación manual)")
            }
        }
        
        // Configurar listener para buffering prolongado
        vlcPlayerManager.setOnBufferingIssueListener {
            println("IPTV: BUFFERING PROLONGADO detectado - Aplicando fix")
            viewModelScope.launch(Dispatchers.Main) {
                pauseResume()
            }
        }
        
        // Configurar listener para inicio de buffering (solo canales de música)
        vlcPlayerManager.setOnBufferingStartListener {
            val currentChannel = _uiState.value.currentChannel
            if (currentChannel != null && isMusicChannel(currentChannel)) {
                println("IPTV: MÚSICA - Buffering detectado - Aplicando fix preventivo inmediato")
                viewModelScope.launch(Dispatchers.Main) {
                    vlcPlayerManager.pausePlayback()
                    delay(800) // Pausa corta
                    vlcPlayerManager.resumePlayback()
                    println("IPTV: MÚSICA - Fix preventivo aplicado")
                }
            }
        }
    }
    
    private fun loadChannels() {
        viewModelScope.launch {
            try {
                allChannels = channelRepository.getAllChannels().first()
                updateChannelsByCategory()
                println("IPTV: Canales cargados: ${allChannels.size}")
            } catch (e: Exception) {
                println("IPTV: Error cargando canales: ${e.message}")
            }
        }
    }
    
    private fun loadCategories() {
        viewModelScope.launch {
            try {
                categoryRepository.getAllCategories().collect { categoriesList ->
                    categories = categoriesList.associate { it.id to it.name }
                    updateChannelsByCategory()
                    println("IPTV: Categorías cargadas: ${categories.size}")
                }
            } catch (e: Exception) {
                println("IPTV: Error cargando categorías: ${e.message}")
            }
        }
    }
    
    private fun updateChannelsByCategory() {
        channelsByCategory = allChannels.groupBy { channel ->
            categories[channel.categoryId] ?: "Sin categoría"
        }
    }

    fun playChannel(channel: Channel) {
        viewModelScope.launch {
            println("IPTV: Iniciando reproducción de: ${channel.name} - URL: ${channel.streamUrl}")
            
            // Asegurar que los datos estén cargados antes de mostrar la información
            ensureDataLoaded()
            
            val categoryName = getCategoryNameForChannel(channel)
            val channelNumber = getChannelNumberForChannel(channel)
            
            _uiState.value = _uiState.value.copy(
                currentChannel = channel,
                isLoading = true,
                error = null,
                showChannelInfo = true,
                showControls = true,
                categoryName = categoryName,
                channelNumber = channelNumber
            )
            
            try {
                // Detener monitores anteriores y reproducción antes de iniciar nueva
                stopMusicMonitor()
                stopVideoMonitor()
                vlcPlayerManager.stop()
                
                vlcPlayerManager.playStream(channel.streamUrl)
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isPlaying = true,
                    categoryName = categoryName,
                    channelNumber = channelNumber
                )
                
                // Iniciar monitor apropiado según tipo de canal
                val isMusic = isMusicChannel(channel)
                println("IPTV: MÚSICA - Verificando canal '${channel.name}' en categoría '${getCategoryNameForChannel(channel)}': ${if (isMusic) "ES MÚSICA" else "NO ES MÚSICA"}")
                
                if (isMusic) {
                    println("IPTV: MÚSICA - Activando monitor agresivo para canal de música: ${channel.name}")
                    startMusicMonitor()
                } else {
                    println("IPTV: VIDEO - Activando monitor de sincronización A/V para: ${channel.name}")
                    startVideoMonitor()
                }
                
                // Ocultar información y controles después de 3 segundos
                kotlinx.coroutines.delay(3000)
                _uiState.value = _uiState.value.copy(
                    showChannelInfo = false,
                    showControls = false
                )
                
                println("IPTV: Reproducción iniciada exitosamente")
            } catch (e: Exception) {
                println("IPTV: Error en playChannel: ${e.message}")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isPlaying = false,
                    error = "Error al reproducir: ${e.message}"
                )
            }
        }
    }

    fun pauseResume() {
        if (_uiState.value.isPlaying) {
            println("IPTV: Pausa manual solicitada")
            vlcPlayerManager.pause()
            _uiState.value = _uiState.value.copy(isPlaying = false)
        } else {
            println("IPTV: Resume manual solicitado")
            vlcPlayerManager.resume()
            _uiState.value = _uiState.value.copy(isPlaying = true)
            
            // Reiniciar monitor si el canal es de música y no está corriendo
            val currentChannel = _uiState.value.currentChannel
            if (currentChannel != null && isMusicChannel(currentChannel)) {
                if (musicMonitorJob?.isActive != true) {
                    println("IPTV: MÚSICA - Reiniciando monitor después de resume manual")
                    startMusicMonitor()
                }
            }
        }
    }

    fun stop() {
        stopMusicMonitor()
        stopVideoMonitor()
        vlcPlayerManager.stop()
        _uiState.value = _uiState.value.copy(
            isPlaying = false,
            currentChannel = null
        )
    }
    
    fun setVolume(volume: Int) {
        vlcPlayerManager.setVolume(volume)
    }
    
    fun getVolume(): Int {
        return vlcPlayerManager.getVolume()
    }
    
    fun nextChannel() {
        viewModelScope.launch {
            if (channelsByCategory.isEmpty()) return@launch
            
            val categoryNames = channelsByCategory.keys.toList()
            if (currentCategoryIndex >= categoryNames.size) return@launch
            
            val currentCategoryName = categoryNames[currentCategoryIndex]
            val channelsInCategory = channelsByCategory[currentCategoryName] ?: return@launch
            
            if (channelsInCategory.isNotEmpty()) {
                // Si estamos en el último canal de la categoría actual
                if (currentChannelInCategoryIndex >= channelsInCategory.size - 1) {
                    // Pasar a la siguiente categoría
                    currentCategoryIndex = (currentCategoryIndex + 1) % categoryNames.size
                    currentChannelInCategoryIndex = 0
                    
                    val newCategoryName = categoryNames[currentCategoryIndex]
                    val newChannelsInCategory = channelsByCategory[newCategoryName] ?: return@launch
                    
                    if (newChannelsInCategory.isNotEmpty()) {
                        val nextChannel = newChannelsInCategory[0]
                        currentChannelIndex = allChannels.indexOfFirst { it.id == nextChannel.id }
                        println("IPTV: Pasando a siguiente categoría '$newCategoryName' - Canal 1/${newChannelsInCategory.size}")
                        playChannel(nextChannel)
                    }
                } else {
                    // Siguiente canal en la misma categoría
                    currentChannelInCategoryIndex++
                    val nextChannel = channelsInCategory[currentChannelInCategoryIndex]
                    currentChannelIndex = allChannels.indexOfFirst { it.id == nextChannel.id }
                    println("IPTV: Siguiente canal en categoría '$currentCategoryName' - ${currentChannelInCategoryIndex + 1}/${channelsInCategory.size}")
                    playChannel(nextChannel)
                }
            }
        }
    }
    
    fun previousChannel() {
        viewModelScope.launch {
            if (channelsByCategory.isEmpty()) return@launch
            
            val categoryNames = channelsByCategory.keys.toList()
            if (currentCategoryIndex >= categoryNames.size) return@launch
            
            val currentCategoryName = categoryNames[currentCategoryIndex]
            val channelsInCategory = channelsByCategory[currentCategoryName] ?: return@launch
            
            if (channelsInCategory.isNotEmpty()) {
                // Si estamos en el primer canal de la categoría actual
                if (currentChannelInCategoryIndex <= 0) {
                    // Pasar a la categoría anterior
                    currentCategoryIndex = if (currentCategoryIndex > 0) {
                        currentCategoryIndex - 1
                    } else {
                        categoryNames.size - 1
                    }
                    
                    val newCategoryName = categoryNames[currentCategoryIndex]
                    val newChannelsInCategory = channelsByCategory[newCategoryName] ?: return@launch
                    
                    if (newChannelsInCategory.isNotEmpty()) {
                        // Ir al último canal de la categoría anterior
                        currentChannelInCategoryIndex = newChannelsInCategory.size - 1
                        val prevChannel = newChannelsInCategory[currentChannelInCategoryIndex]
                        currentChannelIndex = allChannels.indexOfFirst { it.id == prevChannel.id }
                        println("IPTV: Pasando a categoría anterior '$newCategoryName' - Canal ${currentChannelInCategoryIndex + 1}/${newChannelsInCategory.size}")
                        playChannel(prevChannel)
                    }
                } else {
                    // Canal anterior en la misma categoría
                    currentChannelInCategoryIndex--
                    val prevChannel = channelsInCategory[currentChannelInCategoryIndex]
                    currentChannelIndex = allChannels.indexOfFirst { it.id == prevChannel.id }
                    println("IPTV: Canal anterior en categoría '$currentCategoryName' - ${currentChannelInCategoryIndex + 1}/${channelsInCategory.size}")
                    playChannel(prevChannel)
                }
            }
        }
    }
    
    fun setCurrentChannel(channel: Channel) {
        viewModelScope.launch {
            if (allChannels.isEmpty()) {
                allChannels = channelRepository.getAllChannels().first()
            }
            if (categories.isEmpty()) {
                val categoriesList = categoryRepository.getAllCategories().first()
                categories = categoriesList.associate { it.id to it.name }
            }
            updateChannelsByCategory()
            
            val foundIndex = allChannels.indexOfFirst { it.id == channel.id }
            currentChannelIndex = if (foundIndex != -1) foundIndex else 0
            
            // Actualizar índices de categoría
            updateCategoryIndices(channel)
            
            println("IPTV: Canal actual establecido - Índice: $currentChannelIndex, Total: ${allChannels.size}")
            println("IPTV: Categoría: ${getCurrentCategoryName()}, Número: ${getCurrentChannelNumber()}")
        }
    }
    
    private fun updateCategoryIndices(channel: Channel) {
        val categoryName = categories[channel.categoryId] ?: "Sin categoría"
        
        // Si channelsByCategory está vacío, asegurar que se actualice
        if (channelsByCategory.isEmpty()) {
            updateChannelsByCategory()
        }
        
        val categoryNames = channelsByCategory.keys.toList()
        currentCategoryIndex = categoryNames.indexOf(categoryName).takeIf { it >= 0 } ?: 0
        
        val channelsInCategory = channelsByCategory[categoryName] ?: emptyList()
        currentChannelInCategoryIndex = channelsInCategory.indexOfFirst { it.id == channel.id }.takeIf { it >= 0 } ?: 0
        
        println("IPTV: Índices actualizados - Categoría: $currentCategoryIndex ($categoryName), Canal: $currentChannelInCategoryIndex")
    }
    
    fun nextCategory() {
        viewModelScope.launch {
            if (channelsByCategory.isEmpty()) return@launch
            
            val categoryNames = channelsByCategory.keys.toList()
            currentCategoryIndex = (currentCategoryIndex + 1) % categoryNames.size
            currentChannelInCategoryIndex = 0
            
            val newCategoryName = categoryNames[currentCategoryIndex]
            val channelsInCategory = channelsByCategory[newCategoryName] ?: return@launch
            
            if (channelsInCategory.isNotEmpty()) {
                val newChannel = channelsInCategory[0]
                currentChannelIndex = allChannels.indexOfFirst { it.id == newChannel.id }
                playChannel(newChannel)
            }
        }
    }
    
    fun previousCategory() {
        viewModelScope.launch {
            if (channelsByCategory.isEmpty()) return@launch
            
            val categoryNames = channelsByCategory.keys.toList()
            currentCategoryIndex = if (currentCategoryIndex > 0) {
                currentCategoryIndex - 1
            } else {
                categoryNames.size - 1
            }
            currentChannelInCategoryIndex = 0
            
            val newCategoryName = categoryNames[currentCategoryIndex]
            val channelsInCategory = channelsByCategory[newCategoryName] ?: return@launch
            
            if (channelsInCategory.isNotEmpty()) {
                val newChannel = channelsInCategory[0]
                currentChannelIndex = allChannels.indexOfFirst { it.id == newChannel.id }
                playChannel(newChannel)
            }
        }
    }
    
    fun forceReconnect() {
        vlcPlayerManager.forceReconnect()
    }
    
    fun toggleChannelInfo() {
        _uiState.value = _uiState.value.copy(
            showChannelInfo = !_uiState.value.showChannelInfo,
            showControls = !_uiState.value.showControls
        )
    }
    
    fun getCurrentCategoryName(): String {
        val currentChannel = _uiState.value.currentChannel ?: return "Sin categoría"
        return categories[currentChannel.categoryId] ?: "Sin categoría"
    }
    
    fun getCurrentChannelNumber(): String {
        val currentChannel = _uiState.value.currentChannel ?: return "1/1"
        return getChannelNumberForChannel(currentChannel)
    }
    
    private suspend fun ensureDataLoaded() {
        if (allChannels.isEmpty()) {
            allChannels = channelRepository.getAllChannels().first()
        }
        if (categories.isEmpty()) {
            val categoriesList = categoryRepository.getAllCategories().first()
            categories = categoriesList.associate { it.id to it.name }
        }
        updateChannelsByCategory()
    }
    
    private fun getCategoryNameForChannel(channel: Channel): String {
        return categories[channel.categoryId] ?: "Sin categoría"
    }
    
    private fun getChannelNumberForChannel(channel: Channel): String {
        val categoryName = getCategoryNameForChannel(channel)
        val channelsInCategory = allChannels.filter { 
            getCategoryNameForChannel(it) == categoryName 
        }.sortedBy { it.name }
        
        val channelIndex = channelsInCategory.indexOfFirst { it.id == channel.id }
        val position = if (channelIndex >= 0) channelIndex + 1 else 1
        val total = channelsInCategory.size
        
        println("IPTV: Canal ${channel.name} en categoría '$categoryName': $position/$total")
        return "$position/$total"
    }
    
    private fun isMusicChannel(channel: Channel): Boolean {
        val categoryName = getCategoryNameForChannel(channel).lowercase()
        return categoryName.contains("música") || categoryName.contains("music")
    }
    
    private fun startMusicMonitor() {
        stopMusicMonitor() // Detener monitor anterior si existe
        
        lastPosition = 0
        positionCheckCount = 0
        bufferHealthCheckCount = 0
        var frozenCyclesCount = 0
        
        println("IPTV: MÚSICA - ===== Iniciando monitor de transiciones de video =====")
        
        musicMonitorJob = viewModelScope.launch {
            kotlinx.coroutines.delay(4000) // Esperar 4 segundos para estabilización inicial
            
            // Monitor simplificado - detecta solo cambios de video
            // El buffering prolongado se detecta por eventos de VLC
            while (musicMonitorJob?.isActive == true) {
                try {
                    // Si está en pausa manual, solo esperar sin monitorear
                    if (!_uiState.value.isPlaying) {
                        println("IPTV: MÚSICA - Monitor: En pausa, esperando...")
                        frozenCyclesCount = 0 // Resetear contador
                        kotlinx.coroutines.delay(2000)
                        continue
                    }
                    
                    // Obtener posición
                    val currentPosition = kotlinx.coroutines.withTimeoutOrNull(1000) {
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                            try {
                                vlcPlayerManager.mediaPlayer.time
                            } catch (e: Exception) {
                                println("IPTV: MÚSICA - Monitor: Error accediendo VLC: ${e.message}")
                                -1L
                            }
                        }
                    }
                    
                    if (currentPosition == null || currentPosition == -1L) {
                        println("IPTV: MÚSICA - Monitor: ⚠️ Error obteniendo posición de VLC")
                        kotlinx.coroutines.delay(3000)
                        continue
                    }
                    
                    val positionDelta = currentPosition - lastPosition
                    
                    println("IPTV: MÚSICA - Monitor: Pos=${currentPosition}ms (Δ${positionDelta}ms)")
                    
                    // DETECCIÓN: CAMBIO DE VIDEO (posición retrocedió o saltó mucho)
                    // En canales de música, cuando termina un video y empieza otro, la posición puede retroceder
                    if (lastPosition > 0 && (positionDelta < -5000 || positionDelta > 60000)) {
                        println("IPTV: MÚSICA - Monitor: 🎵 CAMBIO DE VIDEO detectado (salto de ${positionDelta}ms) - Aplicando fix preventivo")
                        
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                            vlcPlayerManager.pausePlayback()
                            println("IPTV: MÚSICA - Monitor: ⏸️ PAUSADO (preventivo)")
                            kotlinx.coroutines.delay(1500)
                            vlcPlayerManager.resumePlayback()
                            println("IPTV: MÚSICA - Monitor: ▶️ RESUMIDO - Transición de video asegurada")
                        }
                        
                        lastPosition = currentPosition
                        kotlinx.coroutines.delay(3000)
                        continue
                    }
                    
                    // Actualizar posición
                    if (currentPosition > 0) {
                        lastPosition = currentPosition
                    }
                    
                    // Verificar cada 2 segundos
                    kotlinx.coroutines.delay(2000)
                    
                } catch (e: Exception) {
                    println("IPTV: MÚSICA - Monitor: ❌ Error: ${e.message}")
                    e.printStackTrace()
                    kotlinx.coroutines.delay(3000)
                }
            }
            
            println("IPTV: MÚSICA - ===== Monitor finalizado =====")
        }
    }
    
    private fun stopMusicMonitor() {
        musicMonitorJob?.cancel()
        musicMonitorJob = null
        println("IPTV: MÚSICA - Monitor detenido")
    }
    
    private fun startVideoMonitor() {
        stopVideoMonitor() // Detener monitor anterior si existe
        
        println("IPTV: VIDEO - Iniciando monitor de sincronización A/V")
        
        videoMonitorJob = viewModelScope.launch {
            kotlinx.coroutines.delay(5000) // Esperar 5 segundos para estabilización inicial
            
            var desyncCount = 0
            var frozenCount = 0
            var lastCheckPosition = 0L
            
            while (_uiState.value.isPlaying) {
                try {
                    // Obtener métricas de VLC con timeout
                    val metrics = kotlinx.coroutines.withTimeoutOrNull(1000) {
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                            try {
                                VideoMetrics(
                                    audioDelay = vlcPlayerManager.mediaPlayer.audioDelay,
                                    currentPosition = vlcPlayerManager.mediaPlayer.time,
                                    isPlaying = vlcPlayerManager.mediaPlayer.isPlaying
                                )
                            } catch (e: Exception) {
                                println("IPTV: VIDEO - Error obteniendo métricas: ${e.message}")
                                null
                            }
                        }
                    }
                    
                    if (metrics == null) {
                        println("IPTV: VIDEO - Timeout obteniendo métricas de VLC")
                        kotlinx.coroutines.delay(5000)
                        continue
                    }
                    
                    // 1. Verificar desincronización de audio/video
                    val audioDelayMs = metrics.audioDelay / 1000 // Convertir a ms
                    val isDesynchronized = kotlin.math.abs(audioDelayMs) > 200 // >200ms = problema
                    
                    if (isDesynchronized) {
                        desyncCount++
                        println("IPTV: VIDEO - Desincronización detectada: ${audioDelayMs}ms (${desyncCount}/2)")
                        
                        if (desyncCount >= 2) {
                            println("IPTV: VIDEO - Aplicando corrección de sincronización A/V")
                            applyVideoResync()
                            desyncCount = 0
                            kotlinx.coroutines.delay(5000) // Esperar después del fix
                        }
                    } else {
                        if (desyncCount > 0) {
                            println("IPTV: VIDEO - Sincronización A/V recuperada")
                        }
                        desyncCount = 0
                    }
                    
                    // 2. Verificar congelamiento (posición no avanza)
                    if (metrics.currentPosition > 0 && metrics.currentPosition == lastCheckPosition && metrics.isPlaying) {
                        frozenCount++
                        println("IPTV: VIDEO - Stream congelado detectado (${frozenCount}/3)")
                        
                        if (frozenCount >= 3) {
                            println("IPTV: VIDEO - Stream congelado, reconectando...")
                            applyVideoReconnect()
                            frozenCount = 0
                            lastCheckPosition = 0
                            kotlinx.coroutines.delay(5000)
                        }
                    } else if (metrics.currentPosition > lastCheckPosition) {
                        if (frozenCount > 0) {
                            println("IPTV: VIDEO - Stream recuperado")
                        }
                        frozenCount = 0
                        lastCheckPosition = metrics.currentPosition
                    }
                    
                    // 3. Verificar pérdida de conexión (posición = 0 inesperadamente)
                    if (metrics.currentPosition == 0L && lastCheckPosition > 0) {
                        println("IPTV: VIDEO - Pérdida de conexión detectada")
                        applyVideoReconnect()
                        lastCheckPosition = 0
                        kotlinx.coroutines.delay(5000)
                    }
                    
                    lastAudioDelay = metrics.audioDelay
                    kotlinx.coroutines.delay(5000) // Check cada 5 segundos
                    
                } catch (e: Exception) {
                    println("IPTV: VIDEO - Error en monitor: ${e.message}")
                    kotlinx.coroutines.delay(5000)
                }
            }
            
            println("IPTV: VIDEO - Monitor finalizado")
        }
    }
    
    private fun stopVideoMonitor() {
        videoMonitorJob?.cancel()
        videoMonitorJob = null
        println("IPTV: VIDEO - Monitor detenido")
    }
    
    private suspend fun applyVideoResync() {
        try {
            println("IPTV: VIDEO - Aplicando re-sincronización A/V...")
            
            // Método 1: Reset del delay de audio en VLC
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    vlcPlayerManager.mediaPlayer.setAudioDelay(0) // Reset delay
                    println("IPTV: VIDEO - Audio delay reseteado a 0")
                } catch (e: Exception) {
                    println("IPTV: VIDEO - No se pudo resetear audio delay: ${e.message}")
                }
            }
            
            kotlinx.coroutines.delay(500)
            
            // Método 2: Pausa/Resume corta
            vlcPlayerManager.pause()
            kotlinx.coroutines.delay(600)
            vlcPlayerManager.resume()
            
            println("IPTV: VIDEO - Re-sincronización completada")
            
        } catch (e: Exception) {
            println("IPTV: VIDEO - Error aplicando resync: ${e.message}")
        }
    }
    
    private suspend fun applyVideoReconnect() {
        try {
            val currentChannel = _uiState.value.currentChannel
            if (currentChannel == null) {
                println("IPTV: VIDEO - No hay canal actual para reconectar")
                return
            }
            
            println("IPTV: VIDEO - Reconectando stream...")
            
            kotlinx.coroutines.withTimeoutOrNull(5000) {
                vlcPlayerManager.stop()
                kotlinx.coroutines.delay(500)
                vlcPlayerManager.playStream(currentChannel.streamUrl)
            } ?: run {
                println("IPTV: VIDEO - Timeout en reconexión")
            }
            
        } catch (e: Exception) {
            println("IPTV: VIDEO - Error en reconexión: ${e.message}")
        }
    }
    
    // Función pública para re-sincronización manual
    fun resyncAudioVideo() {
        viewModelScope.launch {
            println("IPTV: VIDEO - Re-sincronización manual iniciada")
            applyVideoResync()
        }
    }
    
    private data class VideoMetrics(
        val audioDelay: Long,
        val currentPosition: Long,
        val isPlaying: Boolean
    )
    
    private suspend fun applyMusicFix() {
        try {
            val currentChannel = _uiState.value.currentChannel
            if (currentChannel == null) {
                println("IPTV: MÚSICA - Error: No hay canal actual para aplicar fix")
                return
            }
            
            val currentPosition = vlcPlayerManager.mediaPlayer.time
            
            // Determinar tipo de problema y aplicar fix correspondiente
            if (currentPosition == 0L) {
                println("IPTV: MÚSICA - Detectada pérdida de conexión, reconectando...")
                
                // Reconexión completa
                kotlinx.coroutines.withTimeoutOrNull(5000) {
                    vlcPlayerManager.stop()
                    kotlinx.coroutines.delay(500)
                    vlcPlayerManager.playStream(currentChannel.streamUrl)
                } ?: run {
                    println("IPTV: MÚSICA - Timeout en reconexión, saltando al siguiente canal")
                    nextChannel()
                    return
                }
            } else {
                println("IPTV: MÚSICA - Video congelado detectado, aplicando fix pausa/resume optimizado...")
                
                // Fix optimizado basado en observación del usuario:
                // Pausa más larga permite que el buffer se limpie y el video se refresque
                vlcPlayerManager.pause()
                kotlinx.coroutines.delay(1200) // Pausa de 1.2 segundos (más efectiva)
                vlcPlayerManager.resume()
                
                println("IPTV: MÚSICA - Fix pausa/resume completado, video debería recuperarse")
            }
            
            println("IPTV: MÚSICA - Fix aplicado exitosamente")
            
        } catch (e: Exception) {
            println("IPTV: MÚSICA - Error aplicando fix: ${e.message}")
            // Intentar reconexión como último recurso
            try {
                val currentChannel = _uiState.value.currentChannel
                if (currentChannel != null) {
                    vlcPlayerManager.playStream(currentChannel.streamUrl)
                }
            } catch (reconnectError: Exception) {
                println("IPTV: MÚSICA - Error en reconexión de emergencia: ${reconnectError.message}")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        println("IPTV: PlayerViewModel destruyéndose - limpiando recursos...")
        
        // Detener monitores
        stopMusicMonitor()
        stopVideoMonitor()
        
        // Liberar player
        vlcPlayerManager.release()
        
        println("IPTV: PlayerViewModel limpiado exitosamente")
    }
}

data class PlayerUiState(
    val currentChannel: Channel? = null,
    val isPlaying: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val showChannelInfo: Boolean = false,
    val showControls: Boolean = false,
    val categoryName: String = "",
    val channelNumber: String = "1/1"
)
