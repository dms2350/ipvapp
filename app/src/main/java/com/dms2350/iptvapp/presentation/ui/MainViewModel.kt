package com.dms2350.iptvapp.presentation.ui

import androidx.lifecycle.ViewModel
import com.dms2350.iptvapp.domain.model.Channel
import com.dms2350.iptvapp.domain.repository.ChannelRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val channelRepository: ChannelRepository
) : ViewModel() {

    fun getChannelById(channelId: Int): Flow<Channel?> {
        return channelRepository.getAllChannels().map { channels ->
            channels.find { it.id == channelId }
        }
    }
}
