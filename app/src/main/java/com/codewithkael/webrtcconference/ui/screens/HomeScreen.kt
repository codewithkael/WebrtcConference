package com.codewithkael.webrtcconference.ui.screens

import android.Manifest
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.codewithkael.webrtcconference.utils.Constants.CONFERENCE_SCREEN
import com.codewithkael.webrtcconference.utils.Constants.DUMMY_ROOM_LIST

@Composable
fun HomeScreen(navController: NavHostController) {
    val context = LocalContext.current
    val requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // All permissions are granted
        if (permissions.all { it.value }) {
            Toast.makeText(context, "shod", Toast.LENGTH_SHORT).show()

            //todo handle onclick later
            navController.navigate(CONFERENCE_SCREEN)
        }
    }
    Column(Modifier.fillMaxWidth()) {
        LazyColumn {
            items(DUMMY_ROOM_LIST) { item ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(1.dp)
                        .border(width = 1.dp, color = Color.Gray, shape = RoundedCornerShape(4.dp))
                        .padding(5.dp)
                        .clickable {
                            requestPermissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.RECORD_AUDIO,
                                    Manifest.permission.CAMERA,
                                )
                            )

                        },
                    horizontalArrangement = Arrangement.SpaceBetween,


                    ) {
                    Text(text = "Room name: ${item.first}")
                    Spacer(modifier = Modifier.padding(5.dp))
                    Text(text = "Members: ${item.second}")

                }
            }
        }
    }

}

@Preview(showBackground = true)
@Composable
fun MainComponentPreview() {
    val navController = rememberNavController()
    HomeScreen(navController = navController)
}