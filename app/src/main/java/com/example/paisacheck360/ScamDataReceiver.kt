package com.example.paisacheck360

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ScamDataReceiver(
    private val onScamDataReceived: (FraudMessage) -> Unit
) : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == "com.example.paisacheck360.SCAM_DATA_UPDATED") {
            val messageType = intent.getStringExtra("type") ?: return
            val messageContent = intent.getStringExtra("content") ?: return
            val timestamp = intent.getLongExtra("timestamp", System.currentTimeMillis())

            val fraudMessage = FraudMessage(messageType, messageContent, timestamp)
            onScamDataReceived(fraudMessage)
        }
    }
}
