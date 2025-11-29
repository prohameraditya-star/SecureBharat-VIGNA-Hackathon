package com.example.paisacheck360

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.ContactsContract
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.content.ContextCompat

class CallReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != "android.intent.action.PHONE_STATE") {
            return
        }

        val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
        Log.d("CallReceiver", "Phone State: $state")

        when (state) {
            TelephonyManager.EXTRA_STATE_RINGING -> {
                val incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)

                if (isNumberInContacts(context, incomingNumber)) {
                    Log.d("CallReceiver", "Number is in contacts. Ignoring.")
                    return
                }

                Log.d("CallReceiver", "RINGING: Sending START command for $incomingNumber")
                val serviceIntent = Intent(context, CallPopupService::class.java).apply {
                    // We only send the number when starting
                    putExtra("callerNumber", incomingNumber)
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
            }

            TelephonyManager.EXTRA_STATE_IDLE, TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                // ❗️ FIX: Call stopService() directly. This is the correct way.
                Log.d("CallReceiver", "IDLE/OFFHOOK: Sending stopService() command.")
                val serviceIntent = Intent(context, CallPopupService::class.java)
                context.stopService(serviceIntent)
            }
        }
    }

    /**
     * Checks if a phone number exists in the user's contacts.
     */
    private fun isNumberInContacts(context: Context, phoneNumber: String?): Boolean {
        if (phoneNumber.isNullOrEmpty()) {
            return false
        }

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            Log.w("CallReceiver", "READ_CONTACTS permission not granted. Cannot check contacts.")
            return false
        }

        val uri = Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            Uri.encode(phoneNumber)
        )
        val projection = arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME)

        try {
            context.contentResolver.query(uri, projection, null, null, null).use { cursor ->
                if (cursor != null && cursor.moveToFirst()) {
                    return true
                }
            }
        } catch (e: Exception) {
            Log.e("CallReceiver", "Error checking contacts", e)
        }

        return false // Number not found
    }
}