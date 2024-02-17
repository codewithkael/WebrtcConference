package com.codewithkael.webrtcconference.remote.socket


interface SocketEventListener {
    fun onNewMessage(message: MessageModel)
    fun onSocketOpened()
    fun onSocketClosed()
}