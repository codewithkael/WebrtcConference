package com.codewithkael.webrtcconference.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.codewithkael.webrtcconference.MainActivity
import com.codewithkael.webrtcconference.R
import com.codewithkael.webrtcconference.remote.socket.MessageModel
import com.codewithkael.webrtcconference.remote.socket.RoomModel
import com.codewithkael.webrtcconference.remote.socket.SocketClient
import com.codewithkael.webrtcconference.remote.socket.SocketEventListener
import com.codewithkael.webrtcconference.remote.socket.SocketEventSender
import com.codewithkael.webrtcconference.remote.socket.SocketEvents.Answer
import com.codewithkael.webrtcconference.remote.socket.SocketEvents.Ice
import com.codewithkael.webrtcconference.remote.socket.SocketEvents.NewSession
import com.codewithkael.webrtcconference.remote.socket.SocketEvents.Offer
import com.codewithkael.webrtcconference.remote.socket.SocketEvents.RoomStatus
import com.codewithkael.webrtcconference.remote.socket.SocketEvents.StartCall
import com.codewithkael.webrtcconference.utils.MyApplication
import com.codewithkael.webrtcconference.webrtc.LocalStreamListener
import com.codewithkael.webrtcconference.webrtc.MyPeerObserver
import com.codewithkael.webrtcconference.webrtc.RTCClient
import com.codewithkael.webrtcconference.webrtc.WebRTCFactory
import com.codewithkael.webrtcconference.webrtc.WebRTCSignalListener
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import org.webrtc.IceCandidate
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.SessionDescription
import org.webrtc.SurfaceViewRenderer
import javax.inject.Inject

@AndroidEntryPoint
class CallService : Service(), SocketEventListener, WebRTCSignalListener {

    @Inject
    lateinit var socketClient: SocketClient

    @Inject
    lateinit var eventSender: SocketEventSender

    @Inject
    lateinit var gson: Gson

    @Inject
    lateinit var webRTCFactory: WebRTCFactory


    //service section
    private lateinit var mainNotification: NotificationCompat.Builder
    private lateinit var notificationManager: NotificationManager

    //state
    val roomsState: MutableStateFlow<List<RoomModel>?> = MutableStateFlow(null)
    val mediaStreamsState: MutableStateFlow<HashMap<String, MediaStream>> = MutableStateFlow(
        hashMapOf()
    )

    private fun getMediaStreams() = mediaStreamsState.value
    fun addMediaStreamToState(username: String, mediaStream: MediaStream) {
        val updatedData = HashMap(getMediaStreams()).apply {
            put(username, mediaStream)
        }
        mediaStreamsState.value = updatedData
    }

    fun removeMediaStreamFromState(username: String) {
        val updatedData = HashMap(getMediaStreams()).apply {
            remove(username)
            Log.d("TAG", "removeMediaStreamFromState: ${mediaStreamsState.value}")
        }
        // Update the state with the new HashMap
        mediaStreamsState.value = updatedData
    }

    //connection list
    private val connections: MutableMap<String, RTCClient> = mutableMapOf()


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            when (it.action) {
                CallServiceActions.START.name -> handleStartService()
                CallServiceActions.STOP.name -> handleStopService()
                else -> Unit
            }
        }
        return START_STICKY
    }

    private fun handleStartService() {
        if (!isServiceRunning) {
            isServiceRunning = true
            //start service here
            startServiceWithNotification()
        }
    }

    private fun handleStopService() {
        if (isServiceRunning) {
            isServiceRunning = false
        }
        socketClient.onStop()
        connections.onEach {
            runCatching {
                it.value.onDestroy()
            }
        }
        webRTCFactory.onDestroy()
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(
            NotificationManager::class.java
        )
        createNotifications()
        socketClient.setListener(this)
    }


    override fun onSocketOpened() {
        eventSender.storeUser()
    }

    override fun onSocketClosed() {
    }

    override fun onNewMessage(message: MessageModel) {
        Log.d("TAG", "onNewMessage: $message")
        when (message.type) {
            RoomStatus -> handleRoomStatus(message)
            NewSession -> handleNewSession(message)
            StartCall -> handleStartCall(message)
            Offer -> handleOffer(message)
            Answer -> handleAnswer(message)
            Ice -> handleIceCandidates(message)
            else -> Unit
        }
    }

    fun initializeSurface(view: SurfaceViewRenderer) {
        webRTCFactory.init(view, object : LocalStreamListener {
            override fun onLocalStreamReady(mediaStream: MediaStream) {
                addMediaStreamToState(MyApplication.username, mediaStream)
            }
        })
    }

    private fun handleNewSession(message: MessageModel) {
        message.name?.let { target ->
            startNewConnection(target) {
                eventSender.startCall(target)
            }
        }
    }

    private fun handleStartCall(message: MessageModel) {
        //we create new connection here
        startNewConnection(message.name!!) {
            it.call()
        }
    }

    private fun startNewConnection(targetName: String, done: (RTCClient) -> Unit) {
        webRTCFactory.createRtcClient(object : MyPeerObserver() {
            override fun onIceCandidate(p0: IceCandidate?) {
                super.onIceCandidate(p0)
                findClient(targetName)?.let {
                    if (p0 != null) {
                        it.sendIceCandidateToPeer(p0, targetName)
                    }
                }
            }

            override fun onAddStream(p0: MediaStream?) {
                Log.d("TAG", "target = $targetName onAddStream: $p0")
                super.onAddStream(p0)
                p0?.let {
                    addMediaStreamToState(targetName, it)
                }
            }

            override fun onConnectionChange(newState: PeerConnection.PeerConnectionState?) {
                super.onConnectionChange(newState)
                Log.d("TAG", "onConnectionChange: $newState")
                if (
                    newState == PeerConnection.PeerConnectionState.CLOSED ||
                    newState == PeerConnection.PeerConnectionState.DISCONNECTED ||
                    newState == PeerConnection.PeerConnectionState.FAILED
                    ) {
                    removeMediaStreamFromState(targetName)
                }
            }
        }, targetName, this).also {
            it?.let {
                connections[targetName] = it
                done(it)
            }
        }
    }

    private fun handleOffer(message: MessageModel) {
        findClient(message.name!!)?.let {
            it.onRemoteSessionReceived(
                SessionDescription(
                    SessionDescription.Type.OFFER,
                    message.data.toString()
                )
            )
            it.answer()
        }
    }

    private fun handleAnswer(message: MessageModel) {
        findClient(message.name!!).apply {
            this?.onRemoteSessionReceived(
                SessionDescription(
                    SessionDescription.Type.ANSWER,
                    message.data.toString()
                )
            )
        }
    }

    private fun handleIceCandidates(message: MessageModel) {
        val ice = runCatching {
            gson.fromJson(message.data.toString(), IceCandidate::class.java)
        }
        Log.d("TAG", "handleIceCandidates: $ice")
        ice.onSuccess {
            findClient(message.name!!).apply {
                Log.d("TAG", "handleIceCandidates: $this")
                this?.addIceCandidateToPeer(it)
            }
        }
    }

    fun leaveRoom(){
        connections.onEach {
            it.value.onDestroy()
        }
    }

    private fun findClient(username: String): RTCClient? {
        return connections[username]
    }


    private fun handleRoomStatus(message: MessageModel) {
        val type = object : TypeToken<List<RoomModel>>() {}.type
        val rooms: List<RoomModel> = gson.fromJson(message.data.toString(), type)

        roomsState.value = rooms
    }


    private fun startServiceWithNotification() {
        startForeground(MAIN_NOTIFICATION_ID, mainNotification.build())
    }

    private val binder = LocalBinder()

    inner class LocalBinder : Binder() {
        fun getService(): CallService = this@CallService
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    private fun createNotifications() {
        val callChannel = NotificationChannel(
            CALL_NOTIFICATION_CHANNEL_ID,
            CALL_NOTIFICATION_CHANNEL_ID,
            NotificationManager.IMPORTANCE_HIGH
        )
        notificationManager.createNotificationChannel(callChannel)
        val contentIntent = Intent(
            this, MainActivity::class.java
        ).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            this,
            System.currentTimeMillis().toInt(),
            contentIntent,
            PendingIntent.FLAG_IMMUTABLE
        )


        val notificationChannel = NotificationChannel(
            "chanel_terminal_bluetooth",
            "chanel_terminal_bluetooth",
            NotificationManager.IMPORTANCE_HIGH
        )


        val intent = Intent(this, CallBroadcastReceiver::class.java).apply {
            action = "ACTION_EXIT"
        }
        val pendingIntent: PendingIntent =
            PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        notificationManager.createNotificationChannel(notificationChannel)
        mainNotification = NotificationCompat.Builder(
            this, "chanel_terminal_bluetooth"
        ).setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOnlyAlertOnce(false)
            .addAction(R.mipmap.ic_launcher, "Exit", pendingIntent)
            .setContentIntent(contentPendingIntent)
    }

    companion object {
        var isServiceRunning = false
        const val CALL_NOTIFICATION_CHANNEL_ID = "CALL_CHANNEL"
        const val MAIN_NOTIFICATION_ID = 2323
        fun startService(context: Context) {
            Thread {
                startIntent(context, Intent(context, CallService::class.java).apply {
                    action = CallServiceActions.START.name
                })
            }.start()
        }

        fun stopService(context: Context) {
            startIntent(context, Intent(context, CallService::class.java).apply {
                action = CallServiceActions.STOP.name
            })
        }

        private fun startIntent(context: Context, intent: Intent) {
            context.startForegroundService(intent)
        }
    }

    override fun onTransferEventToSocket(data: MessageModel) {
        Log.d("TAG", "onTransferEventToSocket: $data")
        socketClient.sendMessageToSocket(data)
    }


}