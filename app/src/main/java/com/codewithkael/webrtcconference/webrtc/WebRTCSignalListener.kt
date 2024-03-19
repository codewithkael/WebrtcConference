package com.codewithkael.webrtcconference.webrtc

import com.codewithkael.webrtcconference.remote.socket.MessageModel


interface WebRTCSignalListener {
    fun onTransferEventToSocket(data: MessageModel)

}