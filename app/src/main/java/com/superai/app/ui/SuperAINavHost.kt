package com.superai.app.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.superai.app.ui.compiler.CompilerScreen
import com.superai.app.ui.dashboard.AgentDetailScreen
import com.superai.app.ui.dashboard.DashboardScreen
import com.superai.app.ui.settings.SettingsScreen
import com.superai.app.ui.storage.StorageScreen
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Dashboard : Screen("dashboard", "Agents",   Icons.Filled.SmartToy)
    object Compiler  : Screen("compiler",  "Compiler", Icons.Filled.Code)
    object Storage   : Screen("storage",   "Storage",  Icons.Filled.FolderOpen)
    object Settings  : Screen("settings",  "Settings", Icons.Filled.Tune)
}

private val bottomNavItems = listOf(Screen.Dashboard, Screen.Compiler, Screen.Storage, Screen.Settings)

@Composable
fun SuperAINavHost() {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            NavigationBar {
                val entry by navController.currentBackStackEntryAsState()
                val current = entry?.destination
                bottomNavItems.forEach { screen ->
                    NavigationBarItem(
                        icon    = { Icon(screen.icon, screen.label) },
                        label   = { Text(screen.label) },
                        selected = current?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController    = navController,
            startDestination = Screen.Dashboard.route,
            modifier         = Modifier.padding(padding)
        ) {
            composable(Screen.Dashboard.route) {
                DashboardScreen(onAgentClick = { id -> navController.navigate("agent_detail/$id") })
            }
            composable("agent_detail/{agentId}") { back ->
                val id = back.arguments?.getString("agentId") ?: return@composable
                AgentDetailScreen(agentId = id, onBack = { navController.popBackStack() })
            }
            composable(Screen.Compiler.route)  { CompilerScreen() }
            composable(Screen.Storage.route)   { StorageScreen() }
            composable(Screen.Settings.route)  { SettingsScreen() }
        }
    }
}
