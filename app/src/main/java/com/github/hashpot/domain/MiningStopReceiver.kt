package com.github.hashpot.domain

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * BroadcastReceiver that handles stopping the mining service and the application.
 */
class MiningStopReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {

        // Stop the mining service
        val serviceIntent = Intent(context, MiningService::class.java)
        context?.stopService(serviceIntent)
    }
}