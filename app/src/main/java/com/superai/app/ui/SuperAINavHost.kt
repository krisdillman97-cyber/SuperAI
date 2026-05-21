package com.superai.app.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.superai.app.ui.compiler.CompilerScreen
import com.superai.app.ui.dashboard.AgentDetailScreen
import com.superai.app.ui.dashboard.DashboardScreen
import com.superai.app.ui.settings.SettingsScreen
import com.superai.app.ui.storage.StorageScreen

sealed class Screen(val route: String) {
    object Dashboard    : Screen("dashboard")
    object Compiler     : Screen("compiler")
    object Storage      : Screen("storage")
    object AgentDetail  : Screen("agent/{profileId}") {
        fun withId(id: String) = "agent/$id"
    }
    object Settings     : Screen("settings?profileId={profileId}") {
        fun withId(id: String?) = "settings?profileId=${id ?: ""}"
    }
}

@Composable
fun SuperAINavHost() {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = Screen.Dashboard.route) {
        composable(Screen.Dashboard.route) {
            DashboardScreen(
                onAgentClick  = { id -> nav.navigate(Screen.AgentDetail.withId(id)) },
                onCompiler    = { nav.navigate(Screen.Compiler.route) },
                onStorage     = { nav.navigate(Screen.Storage.route) },
                onSettings    = { nav.navigate(Screen.Settings.withId(null)) }
            )
        }
        composable(
            route = Screen.AgentDetail.route,
            arguments = listOf(navArgument("profileId") { type = NavType.StringType })
        ) { back ->
            val id = back.arguments?.getString("profileId") ?: return@composable
            AgentDetailScreen(profileId = id,
                onBack     = { nav.popBackStack() },
                onSettings = { nav.navigate(Screen.Settings.withId(id)) })
        }
        composable(Screen.Compiler.route) {
            CompilerScreen(onBack = { nav.popBackStack() })
        }
        composable(Screen.Storage.route) {
            StorageScreen(onBack = { nav.popBackStack() })
        }
        composable(
            route = Screen.Settings.route,
            arguments = listOf(navArgument("profileId") {
                type = NavType.StringType; nullable = true; defaultValue = null
            })
        ) { back ->
            val id = back.arguments?.getString("profileId")?.takeIf { it.isNotBlank() }
            SettingsScreen(profileId = id, onBack = { nav.popBackStack() })
        }
    }
}
