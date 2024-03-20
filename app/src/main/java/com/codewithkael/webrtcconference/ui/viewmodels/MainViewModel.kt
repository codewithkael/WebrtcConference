package com.codewithkael.webrtcconference.ui.viewmodels

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codewithkael.webrtcconference.remote.socket.RoomModel
import com.codewithkael.webrtcconference.remote.socket.SocketEventSender
import com.codewithkael.webrtcconference.service.CallService
import com.codewithkael.webrtcconference.service.CallServiceActions
import com.codewithkael.webrtcconference.webrtc.WebRTCFactory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.webrtc.MediaStream
import org.webrtc.SurfaceViewRenderer
import javax.inject.Inject

@HiltViewModel
@SuppressLint("StaticFieldLeak")
class MainViewModel @Inject constructor(
    private val context: Context,
    private val eventSender: SocketEventSender,
    private val webRTCFactory: WebRTCFactory
) : ViewModel() {

    private lateinit var callService: CallService
    private var isBound = false

    //states
    var roomsState: MutableStateFlow<List<RoomModel>?> = MutableStateFlow(null)
    var mediaStreamsState: MutableStateFlow<HashMap<String, MediaStream>?> = MutableStateFlow(
        hashMapOf()
    )

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as CallService.LocalBinder
            callService = binder.getService()
            isBound = true
            handleServiceBound()
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            isBound = false
        }
    }

    private fun handleServiceBound() {
        callService.roomsState.onEach { rooms ->
            roomsState.emit(rooms)
        }.launchIn(viewModelScope)

        callService.mediaStreamsState.onEach { mediaStreams ->
            Log.d("TAG", "handleServiceBound: $mediaStreams")
            // Directly propagate the state without setting a new value
            mediaStreamsState.emit(mediaStreams)
        }.launchIn(viewModelScope)
    }

    fun onCreateRoomClicked(roomName: String) {
        eventSender.createRoom(roomName)
    }

    fun onRoomClicked(roomName: String,view:SurfaceViewRenderer) {
        callService.initializeSurface(view)
        eventSender.joinRoom(roomName)
    }

    fun init() {
        Intent(context, CallService::class.java).apply {
            action = CallServiceActions.START.name
        }.also { intent ->
            CallService.startService(context)
            context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        }

    }


    override fun onCleared() {
        if (isBound) {
            context.unbindService(serviceConnection)
            isBound = false
        }
        super.onCleared()
    }

    fun onLeaveConferenceClicked() {
        eventSender.leaveAllRooms()
    }

    fun initRemoteSurfaceView(view: SurfaceViewRenderer) {
        webRTCFactory.initRemoteSurfaceView(view)
    }

}