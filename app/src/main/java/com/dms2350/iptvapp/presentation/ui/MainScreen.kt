package com.dms2350.iptvapp.presentation.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.dms2350.iptvapp.domain.model.Channel
import com.dms2350.iptvapp.domain.repository.ChannelRepository
import com.dms2350.iptvapp.presentation.ui.channels.ChannelsScreen
import com.dms2350.iptvapp.presentation.ui.favorites.FavoritesScreen
import com.dms2350.iptvapp.presentation.ui.player.PlayerScreen
import com.dms2350.iptvapp.utils.DeviceUtils
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val context = LocalContext.current
    val isTV = DeviceUtils.isTV(context)

    val bottomNavItems = listOf(
        BottomNavItem("channels", "Canales", Icons.Default.Home),
        BottomNavItem("favorites", "Favoritos", Icons.Default.Favorite)
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            // Ocultar BottomNavigation en TV
            if (!isTV && currentDestination?.route in bottomNavItems.map { it.route }) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        NavigationBarItem(
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) },
                            selected = currentDestination?.hierarchy?.any { it.route == item.route } == true,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "channels",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("channels") {
                ChannelsScreen(
                    onChannelClick = { channel ->
                        navController.navigate("player/${channel.id}")
                    }
                )
            }
            
            composable("favorites") {
                FavoritesScreen(
                    onChannelClick = { channel ->
                        navController.navigate("player/${channel.id}")
                    }
                )
            }
            
            composable("player/{channelId}") { backStackEntry ->
                val channelId = backStackEntry.arguments?.getString("channelId")?.toIntOrNull() ?: 1
                val viewModel: MainViewModel = hiltViewModel()
                val channel by viewModel.getChannelById(channelId).collectAsState(initial = null)
                
                channel?.let {
                    PlayerScreen(
                        channel = it,
                        onBackClick = {
                            navController.popBackStack()
                        }
                    )
                }
            }
        }
    }
}

data class BottomNavItem(
    val route: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)
