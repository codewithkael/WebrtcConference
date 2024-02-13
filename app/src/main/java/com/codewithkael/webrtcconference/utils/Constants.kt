package com.codewithkael.webrtcconference.utils

import java.util.Random

object Constants {
    const val MAIN_SCREEN = "MainScreen"
    const val CONFERENCE_SCREEN = "ConferenceScreen"

    val DUMMY_ROOM_LIST = (1..10).map { "Room$it" to Random().nextInt(5) }
}