package com.iptv.domain.usecase

import com.iptv.domain.model.Channel
import com.iptv.domain.repository.ChannelRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetChannelsUseCase @Inject constructor(
    private val channelRepository: ChannelRepository
) {
    operator fun invoke(): Flow<List<Channel>> {
        return channelRepository.getAllChannels()
    }
}