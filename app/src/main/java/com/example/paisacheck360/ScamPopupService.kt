package com.example.paisacheck360

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import android.widget.*
import androidx.core.app.NotificationCompat
import java.util.ArrayList

class ScamPopupService : Service() {

    private var windowManager: WindowManager? = null
    private var popupView: LinearLayout? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // ❗️ 1. Start Foreground IMMEDIATELY to prevent crash/kill
        startInForeground()

        val sender = intent?.getStringExtra("sender") ?: "Unknown"
        val body = intent?.getStringExtra("body") ?: ""
        val risk = intent?.getStringExtra("risk") ?: "Safe"
        val suggestions = intent?.getStringArrayListExtra("suggestedReplies") ?: arrayListOf("OK")

        // 2. Show Popup
        showPopup(sender, body, risk, suggestions)

        return START_NOT_STICKY
    }

    private fun startInForeground() {
        val channelId = "ScamPopupChannel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Scam Alerts",
                NotificationManager.IMPORTANCE_HIGH
            )
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Secure Bharat Active")
            .setContentText("Scanning messages...")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        startForeground(1, notification)
    }

    private fun showPopup(sender: String, message: String, risk: String, suggestions: ArrayList<String>) {
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        try {
            if (popupView != null) windowManager?.removeView(popupView)
        } catch (_: Exception) {}

        val headerColor = when (risk) {
            "High" -> "#FF5252"
            "Medium" -> "#FFD740"
            "Suspicious" -> "#FFAB40"
            "Low" -> "#81D4FA"
            else -> "#4CAF50"
        }

        popupView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val bg = GradientDrawable()
            bg.setColor(Color.WHITE)
            bg.cornerRadius = 40f
            background = bg
            setPadding(32, 32, 32, 32)
            elevation = 16f
        }

        val header = TextView(this).apply {
            text = "📩 $risk Risk Alert"
            setBackgroundColor(Color.parseColor(headerColor))
            setTextColor(Color.WHITE)
            textSize = 18f
            textAlignment = TextView.TEXT_ALIGNMENT_CENTER
            setPadding(20, 15, 20, 15)
        }
        popupView?.addView(header)

        val senderView = TextView(this).apply {
            text = "From: $sender"
            textSize = 14f
            setTextColor(Color.DKGRAY)
            setPadding(0, 20, 0, 10)
        }
        popupView?.addView(senderView)

        val bodyView = TextView(this).apply {
            text = if (message.length > 200) message.take(200) + "..." else message
            textSize = 16f
            setTextColor(Color.BLACK)
            setPadding(0, 5, 0, 20)
        }
        popupView?.addView(bodyView)

        val btnClose = Button(this).apply {
            text = "Close"
            setBackgroundColor(Color.DKGRAY)
            setTextColor(Color.WHITE)
            setOnClickListener {
                removePopup()
            }
        }
        popupView?.addView(btnClose)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP
        params.y = 100

        try {
            if (Settings.canDrawOverlays(this)) {
                windowManager?.addView(popupView, params)
            } else {
                Log.e("ScamPopupService", "Overlay permission not granted")
            }
        } catch (e: Exception) {
            Log.e("ScamPopupService", "Error adding view", e)
        }
    }

    private fun removePopup() {
        try {
            if (popupView != null) {
                windowManager?.removeView(popupView)
                popupView = null
            }
        } catch (e: Exception) {
            Log.e("ScamPopupService", "Error removing view", e)
        }
        stopSelf()
    }
}