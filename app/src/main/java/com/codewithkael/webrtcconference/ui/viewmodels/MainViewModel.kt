package com.codewithkael.webrtcconference.ui.viewmodels

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.lifecycle.ViewModel
import com.codewithkael.webrtcconference.service.CallService
import com.codewithkael.webrtcconference.service.CallServiceActions
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
@SuppressLint("StaticFieldLeak")
class MainViewModel @Inject constructor(
    private val context: Context
) : ViewModel() {

    private lateinit var callService: CallService
    private var isBound = false

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

    }

    init {
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

}