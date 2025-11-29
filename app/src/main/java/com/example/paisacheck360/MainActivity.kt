package com.example.paisacheck360

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class MainActivity : AppCompatActivity() {

    private lateinit var videoContainer: LinearLayout
    private lateinit var viewAllVideosBtn: Button
    private lateinit var scamSummaryText: TextView
    private lateinit var checkLinkBtn: LinearLayout
    private lateinit var fraudNumberLookupBtn: LinearLayout
    private lateinit var detailedReportBtn: Button
    private lateinit var viewLogsBtn: Button
    private lateinit var appRiskScannerBtn: LinearLayout
    private lateinit var wifiGuardBtn: LinearLayout
    private lateinit var scamCountText: TextView

    private val PERMISSIONS_REQUEST_CODE = 101
    private lateinit var db: DatabaseReference

    private val videos = listOf(
        VideoData("UPI Fraud Prevention", "XKfgdkcIUxw"),
        VideoData("Digital Payment Safety", "IUG2fB4gKKU"),
        VideoData("Phone Scam Alerts", "dQw4w9WgXcQ"),
        VideoData("WhatsApp Scam Prevention", "2Vv-BfVoq4g"),
        VideoData("Online Banking Tips", "fC7oUOUEEi4")
    )

    data class VideoData(val title: String, val youtubeId: String)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initializeViews()
        loadVideos()
        setupClickListeners()
        checkAndRequestPermissions()
        updateScamSummary()
    }

    private fun initializeViews() {
        videoContainer = findViewById(R.id.videoContainer)
        viewAllVideosBtn = findViewById(R.id.viewAllVideos)
        scamSummaryText = findViewById(R.id.appTitle)
        scamCountText = findViewById(R.id.scam_count_text)
        checkLinkBtn = findViewById(R.id.check_link)
        fraudNumberLookupBtn = findViewById(R.id.fraud_number_lookup)
        detailedReportBtn = findViewById(R.id.detailed_report)
        viewLogsBtn = findViewById(R.id.viewLogsBtn)
        appRiskScannerBtn = findViewById(R.id.app_risk_scanner)
        wifiGuardBtn = findViewById(R.id.wifi_guard)
    }

    private fun setupClickListeners() {
        viewAllVideosBtn.setOnClickListener { openYouTubeSearch() }

        checkLinkBtn.setOnClickListener {
            Toast.makeText(this, "Link Checker - Coming Soon!", Toast.LENGTH_SHORT).show()
        }

        fraudNumberLookupBtn.setOnClickListener {
            startActivity(Intent(this, FraudCallSummaryActivity::class.java))
        }

        detailedReportBtn.setOnClickListener {
            startActivity(Intent(this, ScamReportActivity::class.java))
        }

        viewLogsBtn.setOnClickListener {
            startActivity(Intent(this, SmsSummaryActivity::class.java))
        }

        appRiskScannerBtn.setOnClickListener {
            startActivity(Intent(this, AppRiskScannerActivity::class.java))
        }

        wifiGuardBtn.setOnClickListener {
            startActivity(Intent(this, WiFiGuardActivity::class.java))
        }
    }

    private fun loadVideos() {
        videoContainer.removeAllViews()

        videos.forEach { video ->
            val itemLayout = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                val params = LinearLayout.LayoutParams(dpToPx(170), LinearLayout.LayoutParams.WRAP_CONTENT)
                params.setMargins(0, 0, dpToPx(16), 0)
                layoutParams = params
                setPadding(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(8))
                background = getDrawable(android.R.drawable.dialog_frame)
                elevation = 4f
            }

            val thumbnail = ImageView(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(96)
                )
                scaleType = ImageView.ScaleType.CENTER_CROP
            }

            Glide.with(this)
                .load("https://img.youtube.com/vi/${video.youtubeId}/maxresdefault.jpg")
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .placeholder(android.R.drawable.ic_media_play)
                .error(android.R.drawable.stat_notify_error)
                .centerCrop()
                .into(thumbnail)

            thumbnail.setOnClickListener { openYouTubeVideo(video.youtubeId) }

            val title = TextView(this).apply {
                text = video.title
                textSize = 13f
                setTextColor(0xFF333333.toInt())
                maxLines = 2
                setPadding(dpToPx(8), dpToPx(12), dpToPx(8), dpToPx(8))
            }

            itemLayout.addView(thumbnail)
            itemLayout.addView(title)
            videoContainer.addView(itemLayout)
        }
    }

    private fun openYouTubeVideo(videoId: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:$videoId"))
            intent.setPackage("com.google.android.youtube")
            startActivity(intent)
        } catch (e: Exception) {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=$videoId")))
        }
    }

    private fun openYouTubeSearch() {
        val intent = Intent(Intent.ACTION_VIEW,
            Uri.parse("https://www.youtube.com/results?search_query=digital+fraud+prevention+india"))
        startActivity(intent)
    }

    /** ✅ Dynamic scam summary (Firebase) */
    private fun updateScamSummary() {
        val androidID = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID) ?: "guest"
        db = FirebaseDatabase.getInstance("https://sbtest-9acea-default-rtdb.firebaseio.com")
            .reference.child("users").child(androidID).child("alerts")

        db.addValueEventListener(object : ValueEventListener {
            @SuppressLint("SetTextI18n")
            override fun onDataChange(snapshot: DataSnapshot) {
                var high = 0
                var medium = 0
                var low = 0
                var safe = 0

                for (child in snapshot.children) {
                    when (child.child("risk").getValue(String::class.java)) {
                        "High" -> high++
                        "Medium" -> medium++
                        "Low" -> low++
                        else -> safe++
                    }
                }

                scamSummaryText.text = "Secure Bharat – Active Protection"
                scamCountText.text = "📅 Last 7 Days: ${high + medium + low} scams flagged\n" +
                        "🔴 High: $high | 🟡 Medium: $medium | 🔵 Low: $low | 🟢 Safe: $safe"
            }

            @SuppressLint("SetTextI18n")
            override fun onCancelled(error: DatabaseError) {
                scamCountText.text = "Failed to load report."
            }
        })
    }

    /** ✅ Permission checks */
    private fun checkAndRequestPermissions() {
        val permissions = mutableListOf<String>()

        // SMS Permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED)
            permissions.add(Manifest.permission.RECEIVE_SMS)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED)
            permissions.add(Manifest.permission.READ_SMS)

        // Phone Permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED)
            permissions.add(Manifest.permission.READ_PHONE_STATE)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED)
            permissions.add(Manifest.permission.READ_CALL_LOG)

        // Notification Permission (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
                permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        // Request all permissions
        if (permissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissions.toTypedArray(), PERMISSIONS_REQUEST_CODE)
        }

        // Overlay Permission Check
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Log.w("Permissions", "Overlay permission not granted. Asking user.")
            // ✅ FIX: Corrected the typo from MANGE to MANAGE
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            startActivity(intent)
        }
    }

    private fun dpToPx(dp: Int): Int {
        val density = resources.displayMetrics.density
        return (dp * density + 0.5f).toInt()
    }
}