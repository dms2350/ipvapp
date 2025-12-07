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
import timber.log.Timber
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
    private var isApplyingFix = false // Nueva variable para evitar fixes simultáneos
    private var lastMusicFixTime: Long = 0 // Timestamp del último fix aplicado
    private val MUSIC_FIX_COOLDOWN = 8000L // Cooldown de 8 segundos entre fixes
    
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
                Timber.d("IPTV: Canal no funciona - saltando al siguiente")
                nextChannel()
            } else {
                Timber.d("IPTV: Error en canal pero no auto-skip (navegación manual)")
            }
        }
        
        // Configurar listener para buffering prolongado
        vlcPlayerManager.setOnBufferingIssueListener {
            Timber.d("IPTV: BUFFERING PROLONGADO detectado - Aplicando fix")
            viewModelScope.launch(Dispatchers.Main) {
                pauseResume()
            }
        }
        
        // Configurar listener para inicio de buffering (solo canales de música)
        vlcPlayerManager.setOnBufferingStartListener {
            val currentChannel = _uiState.value.currentChannel
            if (currentChannel != null && isMusicChannel(currentChannel) && !isApplyingFix) {
                Timber.d("IPTV: MÚSICA - Buffering detectado - Aplicando fix preventivo inmediato")
                isApplyingFix = true
                viewModelScope.launch(Dispatchers.Main) {
                    try {
                        vlcPlayerManager.pausePlayback()
                        delay(800) // Pausa corta
                        vlcPlayerManager.resumePlayback()
                        Timber.d("IPTV: MÚSICA - Fix preventivo aplicado")
                    } finally {
                        delay(1000) // Esperar 1 segundo adicional
                        isApplyingFix = false
                    }
                }
            } else if (isApplyingFix) {
                Timber.d("IPTV: MÚSICA - Fix ya en progreso, ignorando evento")
            }
        }
    }
    
    private fun loadChannels() {
        viewModelScope.launch {
            try {
                allChannels = channelRepository.getAllChannels().first()
                updateChannelsByCategory()
                Timber.d("IPTV: Canales cargados: ${allChannels.size}")
            } catch (e: Exception) {
                Timber.d("IPTV: Error cargando canales: ${e.message}")
            }
        }
    }
    
    private fun loadCategories() {
        viewModelScope.launch {
            try {
                categoryRepository.getAllCategories().collect { categoriesList ->
                    categories = categoriesList.associate { it.id to it.name }
                    updateChannelsByCategory()
                    Timber.d("IPTV: Categorías cargadas: ${categories.size}")
                }
            } catch (e: Exception) {
                Timber.d("IPTV: Error cargando categorías: ${e.message}")
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
            Timber.d("IPTV: Iniciando reproducción de: ${channel.name} - URL: ${channel.streamUrl}")
            
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
                Timber.d("IPTV: MÚSICA - Verificando canal '${channel.name}' en categoría '${getCategoryNameForChannel(channel)}': ${if (isMusic) "ES MÚSICA" else "NO ES MÚSICA"}")
                
                if (isMusic) {
                    Timber.d("IPTV: MÚSICA - Activando monitor agresivo para canal de música: ${channel.name}")
                    startMusicMonitor()
                } else {
                    Timber.d("IPTV: VIDEO - Activando monitor de sincronización A/V para: ${channel.name}")
                    startVideoMonitor()
                }
                
                // Ocultar información y controles después de 3 segundos
                kotlinx.coroutines.delay(3000)
                _uiState.value = _uiState.value.copy(
                    showChannelInfo = false,
                    showControls = false
                )
                
                Timber.d("IPTV: Reproducción iniciada exitosamente")
            } catch (e: Exception) {
                Timber.d("IPTV: Error en playChannel: ${e.message}")
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
            Timber.d("IPTV: Pausa manual solicitada")
            vlcPlayerManager.pause()
            _uiState.value = _uiState.value.copy(isPlaying = false)
        } else {
            Timber.d("IPTV: Resume manual solicitado")
            vlcPlayerManager.resume()
            _uiState.value = _uiState.value.copy(isPlaying = true)
            
            // Reiniciar monitor si el canal es de música y no está corriendo
            val currentChannel = _uiState.value.currentChannel
            if (currentChannel != null && isMusicChannel(currentChannel)) {
                if (musicMonitorJob?.isActive != true) {
                    Timber.d("IPTV: MÚSICA - Reiniciando monitor después de resume manual")
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
                        Timber.d("IPTV: Pasando a siguiente categoría '$newCategoryName' - Canal 1/${newChannelsInCategory.size}")
                        playChannel(nextChannel)
                    }
                } else {
                    // Siguiente canal en la misma categoría
                    currentChannelInCategoryIndex++
                    val nextChannel = channelsInCategory[currentChannelInCategoryIndex]
                    currentChannelIndex = allChannels.indexOfFirst { it.id == nextChannel.id }
                    Timber.d("IPTV: Siguiente canal en categoría '$currentCategoryName' - ${currentChannelInCategoryIndex + 1}/${channelsInCategory.size}")
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
                        Timber.d("IPTV: Pasando a categoría anterior '$newCategoryName' - Canal ${currentChannelInCategoryIndex + 1}/${newChannelsInCategory.size}")
                        playChannel(prevChannel)
                    }
                } else {
                    // Canal anterior en la misma categoría
                    currentChannelInCategoryIndex--
                    val prevChannel = channelsInCategory[currentChannelInCategoryIndex]
                    currentChannelIndex = allChannels.indexOfFirst { it.id == prevChannel.id }
                    Timber.d("IPTV: Canal anterior en categoría '$currentCategoryName' - ${currentChannelInCategoryIndex + 1}/${channelsInCategory.size}")
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
            
            Timber.d("IPTV: Canal actual establecido - Índice: $currentChannelIndex, Total: ${allChannels.size}")
            Timber.d("IPTV: Categoría: ${getCurrentCategoryName()}, Número: ${getCurrentChannelNumber()}")
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
        
        Timber.d("IPTV: Índices actualizados - Categoría: $currentCategoryIndex ($categoryName), Canal: $currentChannelInCategoryIndex")
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
        
        Timber.d("IPTV: Canal ${channel.name} en categoría '$categoryName': $position/$total")
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
        lastMusicFixTime = 0 // Resetear cooldown al iniciar nuevo monitor
        var frozenCyclesCount = 0
        var stablePlaybackCount = 0
        
        Timber.d("IPTV: MÚSICA - ===== Iniciando monitor de congelamiento agresivo =====")
        Timber.d("IPTV: MÚSICA - Cooldown entre fixes: ${MUSIC_FIX_COOLDOWN}ms (${MUSIC_FIX_COOLDOWN/1000}s)")
        
        musicMonitorJob = viewModelScope.launch {
            kotlinx.coroutines.delay(3000) // Reducido a 3 segundos para detección más rápida
            
            // Monitor agresivo: verifica cada 1 segundo en lugar de 2
            while (musicMonitorJob?.isActive == true) {
                try {
                    // Si está en pausa manual, solo esperar sin monitorear
                    if (!_uiState.value.isPlaying) {
                        Timber.d("IPTV: MÚSICA - Monitor: En pausa, esperando...")
                        frozenCyclesCount = 0
                        stablePlaybackCount = 0
                        kotlinx.coroutines.delay(1000)
                        continue
                    }
                    
                    // Obtener estado de VLC
                    val vlcState = kotlinx.coroutines.withTimeoutOrNull(800) {
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                            try {
                                Pair(
                                    vlcPlayerManager.mediaPlayer.isPlaying,
                                    vlcPlayerManager.mediaPlayer.time
                                )
                            } catch (e: Exception) {
                                Timber.d("IPTV: MÚSICA - Monitor: Error accediendo VLC: ${e.message}")
                                Pair(false, -1L)
                            }
                        }
                    }
                    
                    if (vlcState == null || vlcState.second == -1L) {
                        Timber.d("IPTV: MÚSICA - Monitor: Error obteniendo estado de VLC")
                        kotlinx.coroutines.delay(2000)
                        continue
                    }
                    
                    val (isVlcPlaying, currentPosition) = vlcState
                    val positionDelta = currentPosition - lastPosition
                    val currentTime = System.currentTimeMillis()
                    val timeSinceLastFix = currentTime - lastMusicFixTime
                    val isInCooldown = timeSinceLastFix < MUSIC_FIX_COOLDOWN
                    
                    Timber.d("IPTV: MÚSICA - Monitor: VLC.isPlaying=$isVlcPlaying, Pos=${currentPosition}ms (Δ${positionDelta}ms), Frozen=$frozenCyclesCount, Cooldown=${if(isInCooldown) "${MUSIC_FIX_COOLDOWN - timeSinceLastFix}ms" else "NO"}")
                    
                    // DETECCIÓN 1: CAMBIO DE VIDEO (salto grande de posición)
                    // Reducido el umbral a 30 segundos para detección más rápida
                    if (lastPosition > 0 && (positionDelta < -3000 || positionDelta > 30000)) {
                        if (isInCooldown) {
                            Timber.d("IPTV: MÚSICA - Monitor: CAMBIO DE VIDEO detectado pero EN COOLDOWN (${timeSinceLastFix}ms/${MUSIC_FIX_COOLDOWN}ms) - IGNORANDO para evitar sobrecarga")
                            lastPosition = currentPosition
                            frozenCyclesCount = 0
                            kotlinx.coroutines.delay(1000)
                            continue
                        }
                        
                        Timber.d("IPTV: MÚSICA - Monitor: CAMBIO DE VIDEO detectado (salto de ${positionDelta}ms) - Aplicando fix")
                        lastMusicFixTime = currentTime
                        
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                            vlcPlayerManager.pausePlayback()
                            Timber.d("IPTV: MÚSICA - Monitor: PAUSADO (transición)")
                            kotlinx.coroutines.delay(1500) // Pausa más larga para limpiar decoder
                            vlcPlayerManager.resumePlayback()
                            Timber.d("IPTV: MÚSICA - Monitor: RESUMIDO - Decoder reiniciado")
                        }
                        
                        frozenCyclesCount = 0
                        stablePlaybackCount = 0
                        lastPosition = currentPosition
                        kotlinx.coroutines.delay(3000)
                        continue
                    }
                    
                    // DETECCIÓN 2: VIDEO CONGELADO (posición no avanza)
                    // Reducido a 2 ciclos (2 segundos) en lugar de 3 ciclos (6 segundos)
                    if (isVlcPlaying && currentPosition == lastPosition && currentPosition > 0) {
                        frozenCyclesCount++
                        stablePlaybackCount = 0
                        Timber.d("IPTV: MÚSICA - Monitor: VIDEO CONGELADO detectado (ciclo $frozenCyclesCount/2)")
                        
                        // Si lleva 2 ciclos consecutivos congelado (2 segundos), aplicar fix
                        if (frozenCyclesCount >= 2) {
                            if (isInCooldown) {
                                Timber.d("IPTV: MÚSICA - Monitor: CONGELAMIENTO confirmado pero EN COOLDOWN (${timeSinceLastFix}ms/${MUSIC_FIX_COOLDOWN}ms) - ESPERANDO")
                                kotlinx.coroutines.delay(1000)
                                continue
                            }
                            
                            Timber.d("IPTV: MÚSICA - Monitor: DEADLOCK/CONGELAMIENTO CONFIRMADO - Aplicando fix urgente")
                            lastMusicFixTime = currentTime
                            
                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                vlcPlayerManager.pausePlayback()
                                Timber.d("IPTV: MÚSICA - Monitor: PAUSADO (deadlock)")
                                kotlinx.coroutines.delay(1500) // Pausa larga para limpiar decoder
                                vlcPlayerManager.resumePlayback()
                                Timber.d("IPTV: MÚSICA - Monitor: RESUMIDO - Decoder debería recuperarse")
                            }
                            
                            frozenCyclesCount = 0
                            lastPosition = 0
                            kotlinx.coroutines.delay(3000)
                            continue
                        }
                    }
                    // Posición avanzando normalmente
                    else if (currentPosition > lastPosition) {
                        stablePlaybackCount++
                        
                        if (frozenCyclesCount > 0) {
                            Timber.d("IPTV: MÚSICA - Monitor: Video recuperado (avanzó ${positionDelta}ms)")
                        }
                        
                        frozenCyclesCount = 0
                        lastPosition = currentPosition
                    }
                    // VLC pausado externamente
                    else if (!isVlcPlaying) {
                        frozenCyclesCount = 0
                        stablePlaybackCount = 0
                    }
                    
                    // Verificar cada 1 segundo (más agresivo)
                    kotlinx.coroutines.delay(1000)
                    
                } catch (e: Exception) {
                    Timber.d("IPTV: MÚSICA - Monitor: Error: ${e.message}")
                    e.printStackTrace()
                    frozenCyclesCount = 0
                    kotlinx.coroutines.delay(2000)
                }
            }
            
            Timber.d("IPTV: MÚSICA - ===== Monitor finalizado =====")
        }
    }
    
    private fun stopMusicMonitor() {
        musicMonitorJob?.cancel()
        musicMonitorJob = null
        lastMusicFixTime = 0 // Resetear cooldown al detener monitor
        Timber.d("IPTV: MÚSICA - Monitor detenido")
    }
    
    private fun startVideoMonitor() {
        stopVideoMonitor() // Detener monitor anterior si existe
        
        Timber.d("IPTV: VIDEO - Iniciando monitor de sincronización A/V")
        
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
                                Timber.d("IPTV: VIDEO - Error obteniendo métricas: ${e.message}")
                                null
                            }
                        }
                    }
                    
                    if (metrics == null) {
                        Timber.d("IPTV: VIDEO - Timeout obteniendo métricas de VLC")
                        kotlinx.coroutines.delay(5000)
                        continue
                    }
                    
                    // 1. Verificar desincronización de audio/video
                    val audioDelayMs = metrics.audioDelay / 1000 // Convertir a ms
                    val isDesynchronized = kotlin.math.abs(audioDelayMs) > 200 // >200ms = problema
                    
                    if (isDesynchronized) {
                        desyncCount++
                        Timber.d("IPTV: VIDEO - Desincronización detectada: ${audioDelayMs}ms (${desyncCount}/2)")
                        
                        if (desyncCount >= 2) {
                            Timber.d("IPTV: VIDEO - Aplicando corrección de sincronización A/V")
                            applyVideoResync()
                            desyncCount = 0
                            kotlinx.coroutines.delay(5000) // Esperar después del fix
                        }
                    } else {
                        if (desyncCount > 0) {
                            Timber.d("IPTV: VIDEO - Sincronización A/V recuperada")
                        }
                        desyncCount = 0
                    }
                    
                    // 2. Verificar congelamiento (posición no avanza)
                    if (metrics.currentPosition > 0 && metrics.currentPosition == lastCheckPosition && metrics.isPlaying) {
                        frozenCount++
                        Timber.d("IPTV: VIDEO - Stream congelado detectado (${frozenCount}/3)")
                        
                        if (frozenCount >= 3) {
                            Timber.d("IPTV: VIDEO - Stream congelado, reconectando...")
                            applyVideoReconnect()
                            frozenCount = 0
                            lastCheckPosition = 0
                            kotlinx.coroutines.delay(5000)
                        }
                    } else if (metrics.currentPosition > lastCheckPosition) {
                        if (frozenCount > 0) {
                            Timber.d("IPTV: VIDEO - Stream recuperado")
                        }
                        frozenCount = 0
                        lastCheckPosition = metrics.currentPosition
                    }
                    
                    // 3. Verificar pérdida de conexión (posición = 0 inesperadamente)
                    if (metrics.currentPosition == 0L && lastCheckPosition > 0) {
                        Timber.d("IPTV: VIDEO - Pérdida de conexión detectada")
                        applyVideoReconnect()
                        lastCheckPosition = 0
                        kotlinx.coroutines.delay(5000)
                    }
                    
                    lastAudioDelay = metrics.audioDelay
                    kotlinx.coroutines.delay(5000) // Check cada 5 segundos
                    
                } catch (e: Exception) {
                    Timber.d("IPTV: VIDEO - Error en monitor: ${e.message}")
                    kotlinx.coroutines.delay(5000)
                }
            }
            
            Timber.d("IPTV: VIDEO - Monitor finalizado")
        }
    }
    
    private fun stopVideoMonitor() {
        videoMonitorJob?.cancel()
        videoMonitorJob = null
        Timber.d("IPTV: VIDEO - Monitor detenido")
    }
    
    private suspend fun applyVideoResync() {
        try {
            Timber.d("IPTV: VIDEO - Aplicando re-sincronización A/V...")
            
            // Método 1: Reset del delay de audio en VLC
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    vlcPlayerManager.mediaPlayer.setAudioDelay(0) // Reset delay
                    Timber.d("IPTV: VIDEO - Audio delay reseteado a 0")
                } catch (e: Exception) {
                    Timber.d("IPTV: VIDEO - No se pudo resetear audio delay: ${e.message}")
                }
            }
            
            kotlinx.coroutines.delay(500)
            
            // Método 2: Pausa/Resume corta
            vlcPlayerManager.pause()
            kotlinx.coroutines.delay(600)
            vlcPlayerManager.resume()
            
            Timber.d("IPTV: VIDEO - Re-sincronización completada")
            
        } catch (e: Exception) {
            Timber.d("IPTV: VIDEO - Error aplicando resync: ${e.message}")
        }
    }
    
    private suspend fun applyVideoReconnect() {
        try {
            val currentChannel = _uiState.value.currentChannel
            if (currentChannel == null) {
                Timber.d("IPTV: VIDEO - No hay canal actual para reconectar")
                return
            }
            
            Timber.d("IPTV: VIDEO - Reconectando stream...")
            
            kotlinx.coroutines.withTimeoutOrNull(5000) {
                vlcPlayerManager.stop()
                kotlinx.coroutines.delay(500)
                vlcPlayerManager.playStream(currentChannel.streamUrl)
            } ?: run {
                Timber.d("IPTV: VIDEO - Timeout en reconexión")
            }
            
        } catch (e: Exception) {
            Timber.d("IPTV: VIDEO - Error en reconexión: ${e.message}")
        }
    }
    
    // Función pública para re-sincronización manual
    fun resyncAudioVideo() {
        viewModelScope.launch {
            Timber.d("IPTV: VIDEO - Re-sincronización manual iniciada")
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
                Timber.d("IPTV: MÚSICA - Error: No hay canal actual para aplicar fix")
                return
            }
            
            val currentPosition = vlcPlayerManager.mediaPlayer.time
            
            // Determinar tipo de problema y aplicar fix correspondiente
            if (currentPosition == 0L) {
                Timber.d("IPTV: MÚSICA - Detectada pérdida de conexión, reconectando...")
                
                // Reconexión completa
                kotlinx.coroutines.withTimeoutOrNull(5000) {
                    vlcPlayerManager.stop()
                    kotlinx.coroutines.delay(500)
                    vlcPlayerManager.playStream(currentChannel.streamUrl)
                } ?: run {
                    Timber.d("IPTV: MÚSICA - Timeout en reconexión, saltando al siguiente canal")
                    nextChannel()
                    return
                }
            } else {
                Timber.d("IPTV: MÚSICA - Video congelado detectado, aplicando fix pausa/resume optimizado...")
                
                // Fix optimizado basado en observación del usuario:
                // Pausa más larga permite que el buffer se limpie y el video se refresque
                vlcPlayerManager.pause()
                kotlinx.coroutines.delay(1200) // Pausa de 1.2 segundos (más efectiva)
                vlcPlayerManager.resume()
                
                Timber.d("IPTV: MÚSICA - Fix pausa/resume completado, video debería recuperarse")
            }
            
            Timber.d("IPTV: MÚSICA - Fix aplicado exitosamente")
            
        } catch (e: Exception) {
            Timber.d("IPTV: MÚSICA - Error aplicando fix: ${e.message}")
            // Intentar reconexión como último recurso
            try {
                val currentChannel = _uiState.value.currentChannel
                if (currentChannel != null) {
                    vlcPlayerManager.playStream(currentChannel.streamUrl)
                }
            } catch (reconnectError: Exception) {
                Timber.d("IPTV: MÚSICA - Error en reconexión de emergencia: ${reconnectError.message}")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        Timber.d("IPTV: PlayerViewModel destruyéndose - limpiando recursos...")
        
        // Detener monitores
        stopMusicMonitor()
        stopVideoMonitor()
        
        // Liberar player
        vlcPlayerManager.release()
        
        Timber.d("IPTV: PlayerViewModel limpiado exitosamente")
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

