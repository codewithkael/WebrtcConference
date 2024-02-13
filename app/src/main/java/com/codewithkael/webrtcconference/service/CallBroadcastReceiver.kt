package com.codewithkael.webrtcconference.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.codewithkael.webrtcconference.ui.CloseActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CallBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        intent?.action?.let { action->
            if (action=="ACTION_EXIT"){
                context?.let { noneNullContext ->
                    CallService.stopService(noneNullContext)
                    noneNullContext.startActivity(Intent(noneNullContext, CloseActivity::class.java)
                        .apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        })
                }
            }
        }
    }
}