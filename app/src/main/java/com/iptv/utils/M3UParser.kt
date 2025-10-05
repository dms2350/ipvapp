package com.iptv.utils

import com.iptv.domain.model.Channel

object M3UParser {
    
    fun parseM3U(m3uContent: String): List<Channel> {
        val channels = mutableListOf<Channel>()
        val lines = m3uContent.split("\n")
        
        var currentChannel: Channel? = null
        var channelId = 1
        
        for (i in lines.indices) {
            val line = lines[i].trim()
            
            if (line.startsWith("#EXTINF:")) {
                // Parsear información del canal
                val info = parseExtInf(line)
                val nextLine = if (i + 1 < lines.size) lines[i + 1].trim() else ""
                
                if (nextLine.startsWith("http")) {
                    currentChannel = Channel(
                        id = channelId++,
                        name = info.name,
                        description = info.group,
                        logoUrl = info.logo,
                        streamUrl = nextLine,
                        backupStreamUrl = null,
                        categoryId = null,
                        countryId = null,
                        language = null,
                        quality = "HD",
                        isActive = true,
                        isPremium = false,
                        viewCount = 0
                    )
                    channels.add(currentChannel)
                }
            }
        }
        
        return channels
    }
    
    private fun parseExtInf(line: String): ChannelInfo {
        var name = ""
        var logo = ""
        var group = ""
        
        // Extraer nombre (después de la última coma)
        val lastCommaIndex = line.lastIndexOf(",")
        if (lastCommaIndex != -1 && lastCommaIndex < line.length - 1) {
            name = line.substring(lastCommaIndex + 1).trim()
        }
        
        // Extraer logo
        val logoRegex = """tvg-logo="([^"]*)"""""".toRegex()
        logoRegex.find(line)?.let {
            logo = it.groupValues[1]
        }
        
        // Extraer grupo
        val groupRegex = """group-title="([^"]*)"""""".toRegex()
        groupRegex.find(line)?.let {
            group = it.groupValues[1]
        }
        
        return ChannelInfo(name, logo, group)
    }
    
    private data class ChannelInfo(
        val name: String,
        val logo: String,
        val group: String
    )
}