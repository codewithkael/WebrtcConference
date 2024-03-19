package com.codewithkael.webrtcconference.remote.socket

import com.codewithkael.webrtcconference.remote.socket.SocketEvents.CreateRoom
import com.codewithkael.webrtcconference.remote.socket.SocketEvents.JoinRoom
import com.codewithkael.webrtcconference.remote.socket.SocketEvents.LeaveAllRooms
import com.codewithkael.webrtcconference.remote.socket.SocketEvents.LeaveRoom
import com.codewithkael.webrtcconference.remote.socket.SocketEvents.StartCall
import com.codewithkael.webrtcconference.remote.socket.SocketEvents.StoreUser
import com.codewithkael.webrtcconference.utils.MyApplication
import javax.inject.Inject

class SocketEventSender @Inject constructor(
    private val socketClient: SocketClient
) {

    private var username = MyApplication.username

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
            MessageModel(type = LeaveAllRooms, name = username)
        )
    }

    fun startCall(target: String) {
        socketClient.sendMessageToSocket(
            MessageModel(type = StartCall, name = username, target = target)
        )
    }
}