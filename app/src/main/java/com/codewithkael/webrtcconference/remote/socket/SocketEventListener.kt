package com.codewithkael.webrtcprojectforrecord.utils

import com.codewithkael.webrtcconference.remote.socket.MessageModel

interface SocketEventListener {
    fun onNewMessage(message: MessageModel)
    fun onSocketOpened()
    fun onSocketClosed()
}