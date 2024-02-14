package com.codewithkael.webrtcconference.remote.socket

import com.codewithkael.webrtcconference.remote.socket.SocketEvents.*
import javax.inject.Inject

class SocketEventSender @Inject constructor(
    private val socketClient: SocketClient
) {

    private lateinit var username:String
    private fun getCurrentUser():String = username

    fun storeUser(username:String) {
        this.username = username
        socketClient.sendMessageToSocket(
            MessageModel(type = store_user,name = username)
        )
    }

    fun createRoom(roomName:String){
        socketClient.sendMessageToSocket(
            MessageModel(type = create_room, data = roomName, name = getCurrentUser())
        )
    }

    fun joinRoom(roomName: String){
        socketClient.sendMessageToSocket(
            MessageModel(type = join_room, data = roomName, name = getCurrentUser())
        )
    }

    fun leaveRoom(roomName: String){
        socketClient.sendMessageToSocket(
            MessageModel(type = leave_room, data = roomName, name = getCurrentUser())
        )
    }
}