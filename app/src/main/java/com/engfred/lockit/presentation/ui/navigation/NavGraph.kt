package com.engfred.lockit.presentation.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.engfred.lockit.presentation.ui.screens.AppListScreen
import com.engfred.lockit.presentation.ui.screens.ChangePinScreen
import com.engfred.lockit.presentation.ui.screens.SettingsScreen
import com.engfred.lockit.presentation.ui.screens.SetupScreen

@Composable
fun AppNavGraph(
    navController: NavHostController,
    startDestination: String
) {
    NavHost(navController = navController, startDestination = startDestination) {
        composable("setup") { SetupScreen(navController) }
        composable("app_list") { AppListScreen(navController = navController) }
        composable("settings") { SettingsScreen(navController) }
        composable("change_pin") { ChangePinScreen(navController) }
    }
}