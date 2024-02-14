package com.codewithkael.webrtcconference.remote.socket

import android.util.Log
import com.codewithkael.webrtcprojectforrecord.utils.SocketEventListener
import com.google.gson.Gson
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import java.net.URI
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SocketClient @Inject constructor(
    private val gson: Gson
) {

    private val TAG = "SocketClient"

    private var socketEventListener: SocketEventListener? = null
    fun setListener(messageInterface: SocketEventListener) {
        this.socketEventListener = messageInterface
    }

    fun onStop(){
        socketEventListener = null
        runCatching { webSocket?.closeBlocking() }
    }

    private var webSocket: WebSocketClient? = null

    init {
        initSocket()
    }
    fun initSocket() {
        //if you are using android emulator your local websocket address is going to be "ws://10.0.2.2:3000"
        //if you are using your phone as emulator your local address, use cmd and then write ipconfig
        // and get your ethernet ipv4 , mine is : "ws://192.168.1.3:3000"
        //but if your websocket is deployed you add your websocket address here

        webSocket = object : WebSocketClient(URI("ws://10.0.2.2:3000")) {
            //        webSocket = object : WebSocketClient(URI("ws://192.168.1.3:3000")) {
            override fun onOpen(handshakedata: ServerHandshake?) {
                Log.d(TAG, "onOpen: ")
                socketEventListener?.onSocketOpened()
            }

            override fun onMessage(message: String?) {
                try {
                    socketEventListener?.onNewMessage(gson.fromJson(message, MessageModel::class.java))

                } catch (e: Exception) {
                    e.printStackTrace()
                }

            }

            override fun onClose(code: Int, reason: String?, remote: Boolean) {
                socketEventListener?.onSocketClosed()
                Log.d(TAG, "onClose: $reason")

            }

            override fun onError(ex: Exception?) {
                Log.d(TAG, "onError: ${ex?.message}")
            }

        }
        webSocket?.connect()

    }

    fun sendMessageToSocket(messageModel: MessageModel){
        runCatching {
            webSocket?.send(Gson().toJson(messageModel))
        }
    }


}