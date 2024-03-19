package com.codewithkael.webrtcconference.ui.screens

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.codewithkael.webrtcconference.ui.components.ConfirmBackDialog
import com.codewithkael.webrtcconference.ui.components.SurfaceViewRendererComposable
import com.codewithkael.webrtcconference.ui.viewmodels.MainViewModel
import com.codewithkael.webrtcconference.utils.Constants.MAIN_SCREEN
import com.codewithkael.webrtcconference.utils.MyApplication
import org.webrtc.MediaStream

@Composable
fun ConferenceScreen(roomId:String?,navController: NavHostController, mainViewModel: MainViewModel) {
    val streamState: State<HashMap<String, MediaStream>?> = mainViewModel.mediaStreamsState.collectAsState()
    Log.d("TAG", "ConferenceScreen: $streamState")
    Column(Modifier.fillMaxSize()) {
        Text(text = "room name = $roomId")
        SurfaceViewRendererComposable(modifier = Modifier.fillMaxWidth().height(300.dp)){
            mainViewModel.onRoomClicked(roomId!!,it)
        }
        streamState.value?.forEach { (streamId, mediaStream) ->
            if (streamId != MyApplication.username){
                SurfaceViewRendererComposable(modifier = Modifier.fillMaxWidth().height(300.dp)) {
                    mainViewModel.initRemoteSurfaceView(it)
                    mediaStream.videoTracks[0].addSink(it)
                }
            }
        }
    }

    ConfirmBackDialog{
        mainViewModel::onLeaveConferenceClicked.invoke()
        navController.navigate(MAIN_SCREEN)
    }
}
