package com.codewithkael.webrtcconference.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController

@Composable
fun ConferenceScreen(navController: NavHostController) {
    Column(Modifier.fillMaxSize()) {
        Text(text = "members = user1, user2")


    }
}

@Preview
@Composable
fun Preview(){
    val navController = rememberNavController()
    ConferenceScreen(navController)
}