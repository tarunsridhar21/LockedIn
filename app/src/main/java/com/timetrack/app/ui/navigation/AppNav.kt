package com.timetrack.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.dialog
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.timetrack.app.ui.screens.history.HistoryScreen
import com.timetrack.app.ui.screens.home.HomeScreen
import com.timetrack.app.ui.screens.label.LabelDialog
import com.timetrack.app.ui.screens.settings.SettingsScreen
import com.timetrack.app.ui.screens.stats.StatsScreen

private object Routes {
    const val HOME = "home"
    const val HISTORY = "history"
    const val STATS = "stats"
    const val SETTINGS = "settings"
    const val LABEL = "label/{sessionId}"
    fun label(sessionId: Long) = "label/$sessionId"
}

@Composable
fun AppNav() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val bottomNavRoutes = listOf(Routes.HOME, Routes.HISTORY, Routes.STATS)
    val showBottomBar = currentDestination?.route in bottomNavRoutes

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    NavigationBarItem(
                        selected = currentDestination?.hierarchy?.any { it.route == Routes.HOME } == true,
                        onClick = {
                            navController.navigate(Routes.HOME) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                        label = { Text("Home") },
                    )
                    NavigationBarItem(
                        selected = currentDestination?.hierarchy?.any { it.route == Routes.HISTORY } == true,
                        onClick = {
                            navController.navigate(Routes.HISTORY) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(Icons.Default.History, contentDescription = "History") },
                        label = { Text("History") },
                    )
                    NavigationBarItem(
                        selected = currentDestination?.hierarchy?.any { it.route == Routes.STATS } == true,
                        onClick = {
                            navController.navigate(Routes.STATS) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(Icons.Default.BarChart, contentDescription = "Stats") },
                        label = { Text("Stats") },
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(Routes.HOME) {
                HomeScreen(
                    navController = navController,
                    onNavigateToSettings = { navController.navigate(Routes.SETTINGS) },
                    onNavigateToLabel = { sessionId -> navController.navigate(Routes.label(sessionId)) },
                )
            }
            composable(Routes.HISTORY) {
                HistoryScreen(
                    onNavigateToLabel = { sessionId -> navController.navigate(Routes.label(sessionId)) },
                )
            }
            composable(Routes.STATS) {
                StatsScreen()
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(onBack = { navController.popBackStack() })
            }
            dialog(
                route = Routes.LABEL,
                arguments = listOf(navArgument("sessionId") { type = NavType.LongType }),
            ) { backStackEntry ->
                val sessionId = backStackEntry.arguments?.getLong("sessionId") ?: return@dialog
                LabelDialog(
                    sessionId = sessionId,
                    onDismiss = { navController.popBackStack() },
                )
            }
        }
    }
}
