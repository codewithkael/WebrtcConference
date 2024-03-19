package com.codewithkael.webrtcconference.webrtc

import android.util.Log
import com.codewithkael.webrtcconference.remote.socket.MessageModel
import com.codewithkael.webrtcconference.remote.socket.SocketEvents
import com.google.gson.Gson
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.PeerConnection
import org.webrtc.SessionDescription

class RTCClientImpl(
    private val connection: PeerConnection,
    private val username:String,
    private val target: String,
    private val gson: Gson,
    private var listener: WebRTCSignalListener? = null
) : RTCClient {

    private val mediaConstraint = MediaConstraints().apply {
        mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
        mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
    }

    override val peerConnection = connection
    override fun call() {
        peerConnection.createOffer(object : MySdpObserver() {
            override fun onCreateSuccess(desc: SessionDescription?) {
                Log.d("TAG", "onCreateSuccess: ${desc?.description}")
                super.onCreateSuccess(desc)
                peerConnection.setLocalDescription(object : MySdpObserver() {
                    override fun onSetSuccess() {
                        super.onSetSuccess()
                        listener?.onTransferEventToSocket(
                            MessageModel(
                                type = SocketEvents.Offer, name = username, target =  target,
                                data = desc?.description
                            )
                        )
                    }
                }, desc)
            }
        }, mediaConstraint)
    }

    override fun answer() {
        peerConnection.createAnswer(object : MySdpObserver() {
            override fun onCreateSuccess(desc: SessionDescription?) {
                super.onCreateSuccess(desc)
                peerConnection.setLocalDescription(object : MySdpObserver() {
                    override fun onSetSuccess() {
                        super.onSetSuccess()
                        listener?.onTransferEventToSocket(
                            MessageModel(
                                type = SocketEvents.Answer,
                                name  = username,
                                target = target,
                                data = desc?.description
                            )
                        )
                    }
                }, desc)
            }
        }, mediaConstraint)
    }

    override fun onRemoteSessionReceived(sessionDescription: SessionDescription) {
        peerConnection.setRemoteDescription(MySdpObserver(), sessionDescription)
    }

    override fun addIceCandidateToPeer(iceCandidate: IceCandidate) {
        peerConnection.addIceCandidate(iceCandidate)
    }

    override fun sendIceCandidateToPeer(candidate: IceCandidate, target: String) {
        addIceCandidateToPeer(candidate)
        listener?.onTransferEventToSocket(
            MessageModel(
                type = SocketEvents.Ice,
                name  = username,
                target = target,
                data = gson.toJson(candidate)
            )
        )
    }

    override fun onDestroy() {
        connection.close()
    }
}
