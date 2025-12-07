package com.dms2350.iptvapp.presentation.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.dms2350.iptvapp.data.local.UserPreferences
import com.dms2350.iptvapp.domain.model.Channel
import com.dms2350.iptvapp.presentation.ui.channels.ChannelsScreen
import com.dms2350.iptvapp.presentation.ui.favorites.FavoritesScreen
import com.dms2350.iptvapp.presentation.ui.player.PlayerScreen
import com.dms2350.iptvapp.presentation.ui.registration.RegistrationScreen

@Composable
fun IPTVNavigation(
    navController: NavHostController = rememberNavController(),
    userPreferences: UserPreferences,
    startDestination: String = if (userPreferences.hasCompletedRegistration()) "channels" else "registration"
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable("registration") {
            RegistrationScreen(
                onRegistrationComplete = {
                    navController.navigate("channels") {
                        popUpTo("registration") { inclusive = true }
                    }
                }
            )
        }

        composable("channels") {
            ChannelsScreen(
                onChannelClick = { channel ->
                    // Codificar la URL para evitar problemas con caracteres especiales
                    val encodedUrl = Uri.encode(channel.streamUrl)
                    val encodedName = Uri.encode(channel.name)
                    navController.navigate("player/${channel.id}/$encodedName/$encodedUrl")
                }
            )
        }
        
        composable("favorites") {
            FavoritesScreen(
                onChannelClick = { channel ->
                    // Codificar la URL para evitar problemas con caracteres especiales
                    val encodedUrl = Uri.encode(channel.streamUrl)
                    val encodedName = Uri.encode(channel.name)
                    navController.navigate("player/${channel.id}/$encodedName/$encodedUrl")
                }
            )
        }
        
        composable(
            route = "player/{channelId}/{channelName}/{streamUrl}",
            arguments = listOf(
                navArgument("channelId") { type = NavType.IntType },
                navArgument("channelName") { type = NavType.StringType },
                navArgument("streamUrl") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val channelId = backStackEntry.arguments?.getInt("channelId") ?: 0
            val channelName = backStackEntry.arguments?.getString("channelName")?.let { Uri.decode(it) } ?: ""
            val streamUrl = backStackEntry.arguments?.getString("streamUrl")?.let { Uri.decode(it) } ?: ""

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
