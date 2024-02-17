package com.codewithkael.webrtcconference.ui.nvigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.codewithkael.webrtcconference.ui.screens.ConferenceScreen
import com.codewithkael.webrtcconference.ui.screens.HomeScreen
import com.codewithkael.webrtcconference.ui.viewmodels.MainViewModel
import com.codewithkael.webrtcconference.utils.Constants.MAIN_SCREEN

@Composable
fun AppNavigation(mainViewModel: MainViewModel) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = MAIN_SCREEN) {
        composable(MAIN_SCREEN) {
            HomeScreen(
                navController = navController,
                mainViewModel = mainViewModel
            )
        }
        composable("ConferenceScreen/{roomName}",
            arguments = listOf(navArgument("roomName") { type = NavType.StringType })
        ) {
            ConferenceScreen(
                roomId = it.arguments?.getString("roomName"),
                navController = navController,
                mainViewModel = mainViewModel
            )
        }
    }
}