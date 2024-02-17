package com.codewithkael.webrtcconference.remote.socket

import com.codewithkael.webrtcconference.remote.socket.SocketEvents.*
import java.util.UUID
import javax.inject.Inject

class SocketEventSender @Inject constructor(
    private val socketClient: SocketClient
) {

    private var username = UUID.randomUUID().toString().substring(0,4)

    fun storeUser() {
        socketClient.sendMessageToSocket(
            MessageModel(type = StoreUser, name = username)
        )
    }

    fun createRoom(roomName: String) {
        socketClient.sendMessageToSocket(
            MessageModel(type = CreateRoom, data = roomName, name = username)
        )
    }

    fun joinRoom(roomName: String) {
        socketClient.sendMessageToSocket(
            MessageModel(type = JoinRoom, data = roomName, name = username)
        )
    }

    fun leaveRoom(roomName: String) {
        socketClient.sendMessageToSocket(
            MessageModel(type = LeaveRoom, data = roomName, name = username)
        )
    }

    fun leaveAllRooms() {
        socketClient.sendMessageToSocket(
            MessageModel(type = LeaveAllRooms,name = username)
        )
    }
}