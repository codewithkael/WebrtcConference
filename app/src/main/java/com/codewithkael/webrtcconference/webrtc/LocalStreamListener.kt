package com.codewithkael.webrtcconference.webrtc

import org.webrtc.MediaStream

interface LocalStreamListener {
    fun onLocalStreamReady(mediaStream: MediaStream)
}