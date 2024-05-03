package com.codewithkael.webrtcconference.ui.screens

import android.Manifest
import android.annotation.SuppressLint

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.codewithkael.webrtcconference.ui.viewmodels.MainViewModel
import com.codewithkael.webrtcconference.utils.Constants.conferenceScreen
import com.codewithkael.webrtcconference.utils.RoomNameAlertDialog

@SuppressLint("InlinedApi")
@Composable
fun HomeScreen(navController: NavHostController, mainViewModel: MainViewModel) {


    val context = LocalContext.current
    val createRoomDialog = RoomNameAlertDialog(context, object :
        RoomNameAlertDialog.RoomNameDialogListener {
        override fun onCreateRoomName(roomName: String) {
            mainViewModel.onCreateRoomClicked(roomName)
            navController.navigate(conferenceScreen(roomName))
        }
    })
    val requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // All permissions are granted
        if (permissions.all { it.value }) {
            mainViewModel.init()
        }
    }

    LaunchedEffect(key1 = Unit){
        requestPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.CAMERA,
                Manifest.permission.POST_NOTIFICATIONS,
            )
        )
    }

    val roomState = mainViewModel.roomsState.collectAsState()
    Column(Modifier.fillMaxWidth()) {
        Button(
            onClick = {
                createRoomDialog.show()
            },
            Modifier
                .padding(10.dp)
                .height(40.dp)
                .fillMaxWidth()
        ) {
            Text(text = "Create Room")
        }
        roomState.value?.let { roomList ->
            LazyColumn(Modifier.weight(15f)) {
                items(items = roomList) { item ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(1.dp)
                            .border(
                                width = 1.dp,
                                color = Color.Gray,
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(5.dp)
                            .clickable {
                                navController.navigate(conferenceScreen(item.roomName))
                            },
                        horizontalArrangement = Arrangement.SpaceBetween,


                        ) {
                        Text(text = "Room name: ${item.roomName}")
                        Spacer(modifier = Modifier.padding(5.dp))
                        Text(text = "Members: ${item.population}")

                    }
                }
            }

        }
    }

}

