@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.cocolabshq.ondas.ui

import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.cocolabshq.ondas.MainViewModel
import com.cocolabshq.ondas.data.AppSettings

object Routes {
    const val DISCOVER = "discover"
    const val FAVORITES = "favorites"
    const val RECENTS = "recents"
    const val SETTINGS = "settings"
    const val PLAYER = "player"
}

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab(Routes.DISCOVER, "Descubrir", Icons.Rounded.Explore),
    Tab(Routes.FAVORITES, "Favoritas", Icons.Rounded.Favorite),
    Tab(Routes.RECENTS, "Recientes", Icons.Rounded.History),
    Tab(Routes.SETTINGS, "Ajustes", Icons.Rounded.Settings),
)

@Composable
fun MainScaffold(vm: MainViewModel, settings: AppSettings) {
    val nav = rememberNavController()
    val route = nav.currentBackStackEntryAsState().value?.destination?.route
    val player by vm.player.state.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = Color.Transparent,
        bottomBar = {
            if (route != Routes.PLAYER) {
                Column {
                    MiniPlayer(
                        state = player,
                        onToggle = { vm.player.toggle() },
                        onOpen = { nav.navigate(Routes.PLAYER) { launchSingleTop = true } },
                    )
                    BottomBar(nav, route)
                }
            }
        },
    ) { padding ->
        NavHost(nav, startDestination = Routes.DISCOVER, modifier = Modifier.padding(padding)) {
            composable(Routes.DISCOVER) { DiscoverScreen(vm, settings) }
            composable(Routes.FAVORITES) { FavoritesScreen(vm) }
            composable(Routes.RECENTS) { RecentsScreen(vm) }
            composable(Routes.SETTINGS) { SettingsScreen(vm, settings) }
            composable(
                Routes.PLAYER,
                enterTransition = { slideInVertically(tween(300)) { it } },
                popExitTransition = { slideOutVertically(tween(300)) { it } },
            ) { PlayerScreen(vm) { nav.popBackStack() } }
        }
    }
}

@Composable
private fun BottomBar(nav: NavHostController, route: String?) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)) {
        tabs.forEach { t ->
            NavigationBarItem(
                selected = route == t.route,
                onClick = {
                    nav.navigate(t.route) {
                        popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Icon(t.icon, contentDescription = t.label) },
                label = { Text(t.label) },
            )
        }
    }
}
