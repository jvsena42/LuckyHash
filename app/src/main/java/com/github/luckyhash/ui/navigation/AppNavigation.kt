package com.github.luckyhash.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.github.luckyhash.ui.screens.config.ConfigScreen

@Composable
fun AppNavHost(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Screen.Stats.route) {
        composable(Screen.Stats.route) {
            StatsScreen(
                onNavigateToConfig = { navController.navigate(Screen.Config.route) }
            )
        }
        composable(Screen.Config.route) {
            ConfigScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}

sealed class Screen(val route: String) {
    object Stats : Screen("stats")
    object Config : Screen("config")
}