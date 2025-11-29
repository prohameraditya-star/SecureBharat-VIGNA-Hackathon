package com.example.paisacheck360

import android.content.Intent
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.json.JSONObject
import java.io.IOException

class SMSNotificationListener : NotificationListenerService() {

    private val client = OkHttpClient()
    private val apiUrl = "https://backend-k0ri.onrender.com/predict"

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d("SMSNotification", "✅ Notification listener connected and ready")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return

        val pkg = sbn.packageName ?: "unknown"
        val title = sbn.notification.extras.getString("android.title") ?: "Unknown"
        val text = sbn.notification.extras.getCharSequence("android.text")?.toString() ?: ""

        Log.d("SMSNotification", "🔔 New Notification → pkg: $pkg | title: $title | text: $text")

        // ✅ Detect SMS-like apps dynamically
        if (pkg.contains("messaging", true) ||
            pkg.contains("sms", true) ||
            pkg.contains("mms", true) ||
            pkg.contains("msg", true) ||
            pkg.contains("message", true) ||
            pkg.contains("inbox", true) ||
            pkg.contains("whatsapp", true)) {

            Log.d("SMSNotification", "📩 SMS-like notification caught from $pkg")

            // ✅ Send it for analysis
            processMessage(title, text)
        }
    }

    private fun processMessage(sender: String, message: String) {
        Log.d("SMSNotification", "📤 Processing message from $sender: $message")

        val json = JSONObject().apply { put("message", message) }
        val body = RequestBody.create("application/json".toMediaTypeOrNull(), json.toString())
        val req = Request.Builder().url(apiUrl).post(body).build()

        client.newCall(req).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("SMSNotification", "❌ API call failed: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                val resStr = response.body?.string()
                Log.d("SMSNotification", "🤖 API response: $resStr")

                try {
                    val jsonRes = JSONObject(resStr ?: "{}")
                    val isScam = jsonRes.optBoolean("is_scam", false)

                    if (isScam) {
                        Log.d("SMSNotification", "🚨 Scam detected from: $sender")

                        // ✅ Trigger popup alert
                        val popupIntent = Intent(applicationContext, ScamPopupService::class.java).apply {
                            putExtra("sender", sender)
                            putExtra("message", message)
                            putExtra("risk", "High")
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        startService(popupIntent)
                    }
                } catch (e: Exception) {
                    Log.e("SMSNotification", "⚠️ Error parsing API response", e)
                }
            }
        })
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        Log.d("SMSNotification", "🗑️ Notification removed for pkg: ${sbn?.packageName}")
    }
}
