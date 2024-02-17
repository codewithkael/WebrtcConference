package com.codewithkael.webrtcconference.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.codewithkael.webrtcconference.MainActivity
import com.codewithkael.webrtcconference.R
import com.codewithkael.webrtcconference.remote.socket.MessageModel
import com.codewithkael.webrtcconference.remote.socket.RoomModel
import com.codewithkael.webrtcconference.remote.socket.SocketClient
import com.codewithkael.webrtcconference.remote.socket.SocketEventListener
import com.codewithkael.webrtcconference.remote.socket.SocketEventSender
import com.codewithkael.webrtcconference.remote.socket.SocketEvents.RoomStatus
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject

@AndroidEntryPoint
class CallService : Service(), SocketEventListener {

    @Inject
    lateinit var socketClient: SocketClient
    @Inject
    lateinit var eventSender: SocketEventSender
    @Inject lateinit var gson: Gson

    //service section
    private lateinit var mainNotification: NotificationCompat.Builder
    private lateinit var notificationManager: NotificationManager

    //state
    val roomsState: MutableStateFlow<List<RoomModel>?> = MutableStateFlow(null)


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
        when (message.type) {
            RoomStatus -> handleRoomStatus(message)
            else -> Unit
        }
    }

    private fun handleRoomStatus(message: MessageModel) {
        val type = object : TypeToken<List<RoomModel>>() {}.type
        val rooms: List<RoomModel> =gson.fromJson(message.data.toString(), type)

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


}