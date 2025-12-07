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
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.dms2350.iptvapp.data.local.UserPreferences
import com.dms2350.iptvapp.presentation.navigation.IPTVNavigation
import com.dms2350.iptvapp.utils.DeviceUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(userPreferences: UserPreferences) {
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
            // Ocultar BottomNavigation en TV y en pantalla de registro
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
    ) { paddingValues ->
        IPTVNavigation(
            navController = navController,
            userPreferences = userPreferences
        )
    }
}

data class BottomNavItem(
    val route: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)
