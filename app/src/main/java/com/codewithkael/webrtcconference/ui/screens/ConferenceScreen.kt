package com.codewithkael.webrtcconference.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import com.codewithkael.webrtcconference.ui.components.ConfirmBackDialog
import com.codewithkael.webrtcconference.ui.components.SurfaceViewRendererComposable
import com.codewithkael.webrtcconference.ui.viewmodels.MainViewModel
import com.codewithkael.webrtcconference.utils.Constants.MAIN_SCREEN
import com.codewithkael.webrtcconference.utils.MyApplication

@Composable
fun ConferenceScreen(
    roomId: String?,
    navController: NavHostController,
    mainViewModel: MainViewModel
) {
    val streamState = mainViewModel.mediaStreamsState.collectAsState().value ?: hashMapOf()

    // Total number of streams includes the local stream plus the number of remote streams
    val totalNumberOfStreams = 1 + streamState.count { it.key != MyApplication.username }

    Column(Modifier.fillMaxSize()) {
        Text(text = "room name = $roomId")

        // Calculate the modifier for each stream so they share the space equally
        val streamModifier = Modifier
            .fillMaxWidth()
            .weight(1f / totalNumberOfStreams) // Divide space equally among all streams

        // Render the local stream
        SurfaceViewRendererComposable(
            modifier = streamModifier,
            streamName = "Local",
            onSurfaceReady = { mainViewModel.onRoomClicked(roomId!!, it) }
        )

        // Render each remote stream
        streamState.forEach { (streamId, mediaStream) ->
            if (streamId != MyApplication.username) {
                // Use the key composable to manage recomposition based on streamId
                key(streamId) {
                    SurfaceViewRendererComposable(
                        modifier = streamModifier,
                        streamName = streamId,
                        onSurfaceReady = { surfaceView ->
                            mainViewModel.initRemoteSurfaceView(surfaceView)
                            mediaStream.videoTracks.firstOrNull()?.addSink(surfaceView)
                        }
                    )
                }
            }
        }
    }

    ConfirmBackDialog {
        mainViewModel::onLeaveConferenceClicked.invoke()
        navController.navigate(MAIN_SCREEN)
    }
}

