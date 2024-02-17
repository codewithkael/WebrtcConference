package com.codewithkael.webrtcconference.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import com.codewithkael.webrtcconference.ui.components.ConfirmBackDialog
import com.codewithkael.webrtcconference.ui.viewmodels.MainViewModel
import com.codewithkael.webrtcconference.utils.Constants.MAIN_SCREEN

@Composable
fun ConferenceScreen(navController: NavHostController, mainViewModel: MainViewModel) {
    Column(Modifier.fillMaxSize()) {
        Text(text = "members = user1, user2")
    }

    ConfirmBackDialog{
        mainViewModel::onLeaveConferenceClicked.invoke()
        navController.navigate(MAIN_SCREEN)
    }
}
