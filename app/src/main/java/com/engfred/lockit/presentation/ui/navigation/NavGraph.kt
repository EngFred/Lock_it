package com.engfred.lockit.presentation.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.engfred.lockit.presentation.ui.screens.AppListScreen
import com.engfred.lockit.presentation.ui.screens.SetupScreen

@Composable
fun AppNavGraph(
    navController: NavHostController,
    startDestination: String
) {
    NavHost(navController = navController, startDestination = startDestination) {
        composable("setup") { SetupScreen(navController) }
        composable("app_list") { AppListScreen() }
    }
}