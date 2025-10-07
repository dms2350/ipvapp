package com.dms2350.iptvapp.domain.usecase

import com.dms2350.iptvapp.domain.model.Channel
import com.dms2350.iptvapp.domain.repository.ChannelRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetChannelsUseCase @Inject constructor(
    private val channelRepository: ChannelRepository
) {
    operator fun invoke(): Flow<List<Channel>> {
        return channelRepository.getAllChannels()
    }
}
