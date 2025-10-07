package com.dms2350.iptvapp.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.dms2350.iptvapp.domain.model.Channel
import com.dms2350.iptvapp.presentation.ui.channels.ChannelsScreen
import com.dms2350.iptvapp.presentation.ui.favorites.FavoritesScreen
import com.dms2350.iptvapp.presentation.ui.player.PlayerScreen

@Composable
fun IPTVNavigation(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = "channels"
    ) {
        composable("channels") {
            ChannelsScreen(
                onChannelClick = { channel ->
                    navController.navigate("player/${channel.id}/${channel.name}/${channel.streamUrl}")
                }
            )
        }
        
        composable("favorites") {
            FavoritesScreen(
                onChannelClick = { channel ->
                    navController.navigate("player/${channel.id}/${channel.name}/${channel.streamUrl}")
                }
            )
        }
        
        composable("player/{channelId}/{channelName}/{streamUrl}") { backStackEntry ->
            val channelId = backStackEntry.arguments?.getString("channelId")?.toIntOrNull() ?: 0
            val channelName = backStackEntry.arguments?.getString("channelName") ?: ""
            val streamUrl = backStackEntry.arguments?.getString("streamUrl") ?: ""
            
            val channel = Channel(
                id = channelId,
                name = channelName,
                description = null,
                logoUrl = null,
                streamUrl = streamUrl,
                backupStreamUrl = null,
                categoryId = null,
                countryId = null,
                language = null
            )
            
            PlayerScreen(
                channel = channel,
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }
    }
}
