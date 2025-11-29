package com.example.paisacheck360

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*

class CallPopupService : Service() {

    private lateinit var windowManager: WindowManager
    private var popupView: View? = null
    private var callerNumber: String = "Unknown"
    private lateinit var db: DatabaseReference

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        val androidID = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID) ?: "guest"

        // ❗️✅ FIX 1: Corrected URL (changed 'https.' to 'https://')
        db = FirebaseDatabase.getInstance("https://sbtest-9acea-default-rtdb.firebaseio.com")
            .reference.child("users").child(androidID).child("call_feedback")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        if (intent?.action == "ACTION_STOP") {
            Log.d("CallPopupService", "Received STOP action. Shutting down.")
            stopSelf()
            return START_NOT_STICKY
        }

        val numberFromIntent = intent?.getStringExtra("callerNumber")
        callerNumber = if (numberFromIntent.isNullOrEmpty()) {
            "Unknown"
        } else {
            numberFromIntent
        }

        startInForeground()

        if (popupView == null) {
            checkNumberStatus(callerNumber)
        } else {
            Log.w("CallPopupService", "Popup is already showing, ignoring new call.")
        }

        return START_NOT_STICKY
    }

    private fun checkNumberStatus(number: String) {
        val last10Digits = if (number.length > 10) number.takeLast(10) else number

        db.orderByKey().addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var entryFound: DataSnapshot? = null

                for (child in snapshot.children) {
                    val dbKey = child.key
                    if (dbKey != null && dbKey.takeLast(10) == last10Digits) {
                        entryFound = child
                        break
                    }
                }

                if (entryFound != null) {
                    val status = entryFound.child("status").getValue(String::class.java)

                    if (status == "Scam") {
                        Log.d("CallPopupService", "Number $number is a known scam. Showing alert.")
                        showPopup(number, isKnownScam = true)
                    } else if (status == "Safe") {
                        Log.d("CallPopupService", "Number $number is known and safe. Ignoring.")
                        stopSelf()
                    } else {
                        Log.d("CallPopupService", "Number $number found but status is '$status'. Showing popup.")
                        showPopup(number, isKnownScam = false)
                    }
                } else {
                    Log.d("CallPopupService", "Number $number is not in DB. Showing popup.")
                    showPopup(number, isKnownScam = false)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("CallPopupService", "Firebase check failed: ${error.message}")
                showPopup(number, isKnownScam = false)
            }
        })
    }

    private fun showPopup(number: String, isKnownScam: Boolean) {
        if (popupView != null) {
            Log.w("CallPopupService", "Popup is already showing. Ignoring new request.")
            return
        }

        popupView = LayoutInflater.from(this).inflate(R.layout.activity_call_feedback, null)

        popupView?.let { view ->
            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    WindowManager.LayoutParams.TYPE_PHONE
                },
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT
            )
            params.gravity = Gravity.CENTER

            val txtNumber = view.findViewById<TextView>(R.id.txtNumber)
            val btnScam = view.findViewById<Button>(R.id.btnScam)
            val btnSafe = view.findViewById<Button>(R.id.btnSafe)

            if (isKnownScam) {
                txtNumber.text = "🚨 KNOWN SCAM CALL 🚨\n$number"
                btnScam.visibility = View.GONE

                btnSafe.visibility = View.VISIBLE
                btnSafe.text = "OK"
                btnSafe.setOnClickListener {
                    removePopup()
                    stopSelf()
                }
            } else {
                txtNumber.text = "Incoming Call: $number"
                btnScam.visibility = View.VISIBLE
                btnSafe.visibility = View.VISIBLE
                btnSafe.text = "✅ Mark as Safe"

                btnScam.setOnClickListener {
                    saveFeedback(number, "Scam")
                    removePopup()
                    stopSelf()
                }
                btnSafe.setOnClickListener {
                    saveFeedback(number, "Safe")
                    removePopup()
                    stopSelf()
                }
            }

            try {
                if (Settings.canDrawOverlays(this)) {
                    windowManager.addView(view, params)
                } else {
                    Log.e("CallPopupService", "Error: SYSTEM_ALERT_WINDOW permission not granted!")
                }
            } catch (e: Exception) {
                Log.e("CallPopupService", "Error adding view to WindowManager", e)
            }
        }
    }

    private fun removePopup() {
        popupView?.let {
            try {
                windowManager.removeView(it)
                popupView = null
            } catch (e: Exception) {
                Log.e("CallPopupService", "Error removing popup view", e)
            }
        }
    }

    private fun saveFeedback(number: String, feedback: String) {
        val androidID = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
        if (androidID.isNullOrEmpty()) {
            Log.e("CallPopupService", "Could not get Android ID")
            return
        }

        // ❗️✅ FIX 2: Corrected URL here as well
        val dbRef = FirebaseDatabase.getInstance("https://sbtest-9acea-default-rtdb.firebaseio.com")
            .reference.child("users").child(androidID).child("call_feedback")

        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        val callData = mapOf("status" to feedback, "timestamp" to timestamp)
        val safeNumberKey = number.replace(Regex("[.#$\\[\\]]"), "_")

        dbRef.child(safeNumberKey).setValue(callData)
            .addOnSuccessListener { Log.d("CallPopupService", "Feedback saved") }
            .addOnFailureListener { e -> Log.e("CallPopupService", "Failed to save feedback", e) }
    }

    private fun startInForeground() {
        val channelId = "CallPopupChannel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Call Popup Service",
                NotificationManager.IMPORTANCE_LOW
            )
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("PaisaCheck360 Active")
            .setContentText("Listening for scam calls.")
            .setSmallIcon(R.mipmap.ic_launcher)
            .build()

        startForeground(2, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        removePopup()
    }
}