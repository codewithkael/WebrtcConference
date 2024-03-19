package com.codewithkael.webrtcconference.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import com.codewithkael.webrtcconference.ui.components.ConfirmBackDialog
import com.codewithkael.webrtcconference.ui.components.SurfaceViewRendererComposable
import com.codewithkael.webrtcconference.ui.viewmodels.MainViewModel
import com.codewithkael.webrtcconference.utils.Constants.MAIN_SCREEN

@Composable
fun ConferenceScreen(roomId:String?,navController: NavHostController, mainViewModel: MainViewModel) {
    Column(Modifier.fillMaxSize()) {
        Text(text = "room name = $roomId")
        SurfaceViewRendererComposable(modifier = Modifier.fillMaxSize()){
            mainViewModel.onRoomClicked(roomId!!,it)
        }
    }

    ConfirmBackDialog{
        mainViewModel::onLeaveConferenceClicked.invoke()
        navController.navigate(MAIN_SCREEN)
    }


}
