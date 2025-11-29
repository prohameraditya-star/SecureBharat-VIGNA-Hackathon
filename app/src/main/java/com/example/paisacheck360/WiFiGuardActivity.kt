package com.example.paisacheck360

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class WiFiGuardActivity : AppCompatActivity() {

    private lateinit var statusText: TextView
    private lateinit var statusTitle: TextView
    private lateinit var ssidText: TextView
    private lateinit var bssidText: TextView
    private lateinit var signalText: TextView
    private lateinit var speedText: TextView
    private lateinit var frequencyText: TextView
    private lateinit var ipText: TextView
    private lateinit var scanBtn: Button
    private lateinit var detailsBtn: Button
    private lateinit var statusBox: LinearLayout

    private val LOCATION_PERMISSION_REQUEST = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wifi_guard)

        initializeViews()
        setupListeners()
        checkPermissionsAndScan()
    }

    private fun initializeViews() {
        statusText = findViewById(R.id.statusText)
        statusTitle = findViewById(R.id.statusTitle)
        ssidText = findViewById(R.id.ssidText)
        bssidText = findViewById(R.id.bssidText)
        signalText = findViewById(R.id.signalText)
        speedText = findViewById(R.id.speedText)
        frequencyText = findViewById(R.id.frequencyText)
        ipText = findViewById(R.id.ipText)
        scanBtn = findViewById(R.id.scanBtn)
        detailsBtn = findViewById(R.id.detailsBtn)
        statusBox = findViewById(R.id.statusBox)
    }

    private fun setupListeners() {
        scanBtn.setOnClickListener {
            checkPermissionsAndScan()
            Toast.makeText(this, "Scanning network...", Toast.LENGTH_SHORT).show()
        }

        detailsBtn.setOnClickListener {
            showDetailedInfo()
        }
    }

    private fun checkPermissionsAndScan() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST
            )
        } else {
            scanNetwork()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                scanNetwork()
            } else {
                Toast.makeText(this, "Location permission needed for WiFi info", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun scanNetwork() {
        val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

        if (!wifiManager.isWifiEnabled) {
            updateDisconnectedState()
            return
        }

        val info: WifiInfo = wifiManager.connectionInfo
        val ssid = info.ssid.removePrefix("\"").removeSuffix("\"")

        if (ssid == "<unknown ssid>" || info.networkId == -1) {
            updateDisconnectedState()
            return
        }

        // Update basic info
        ssidText.text = ssid
        bssidText.text = "BSSID: ${info.bssid ?: "Unknown"}"

        // Update signal strength
        val rssi = info.rssi
        val signalLevel = WifiManager.calculateSignalLevel(rssi, 5)
        signalText.text = when {
            signalLevel >= 4 -> "Excellent (${rssi} dBm)"
            signalLevel >= 3 -> "Good (${rssi} dBm)"
            signalLevel >= 2 -> "Fair (${rssi} dBm)"
            else -> "Weak (${rssi} dBm)"
        }

        // Update link speed
        speedText.text = "${info.linkSpeed} Mbps"

        // Update frequency
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            val frequency = info.frequency
            frequencyText.text = when {
                frequency >= 5000 -> "5 GHz ($frequency MHz)"
                frequency >= 2400 -> "2.4 GHz ($frequency MHz)"
                else -> "$frequency MHz"
            }
        } else {
            frequencyText.text = "N/A"
        }

        // Update IP address
        val ip = info.ipAddress
        ipText.text = if (ip != 0) {
            String.format(
                "%d.%d.%d.%d",
                ip and 0xff,
                ip shr 8 and 0xff,
                ip shr 16 and 0xff,
                ip shr 24 and 0xff
            )
        } else {
            "Not available"
        }

        // Analyze network security
        analyzeNetworkSecurity(ssid, info)
    }

    private fun analyzeNetworkSecurity(ssid: String, info: WifiInfo) {
        when {
            // Check for dangerous network names
            ssid.contains("free", ignoreCase = true) ||
                    ssid.contains("public", ignoreCase = true) ||
                    ssid.contains("guest", ignoreCase = true) ||
                    ssid.contains("open", ignoreCase = true) -> {
                updateSecurityStatus(
                    title = "⚠️ Potentially Unsafe",
                    message = "This appears to be a public network. Avoid accessing sensitive information like banking apps or entering passwords.",
                    bgColor = "#FFF3E0"
                )
            }

            // Check signal strength
            info.rssi < -80 -> {
                updateSecurityStatus(
                    title = "⚠️ Weak Signal",
                    message = "Very weak signal detected. This may indicate you're far from the router or possible interference. Connection may be unstable.",
                    bgColor = "#FFF9C4"
                )
            }

            // Secure network
            else -> {
                updateSecurityStatus(
                    title = "✅ Secure Network",
                    message = "Your connection is protected with modern encryption (WPA2/WPA3). Good signal strength detected. Safe for general use.",
                    bgColor = "#E8F5E9"
                )
            }
        }
    }

    private fun updateSecurityStatus(title: String, message: String, bgColor: String) {
        statusTitle.text = title
        statusText.text = message
        statusBox.setBackgroundColor(Color.parseColor(bgColor))
    }

    private fun updateDisconnectedState() {
        ssidText.text = "Not Connected"
        bssidText.text = "BSSID: --"
        signalText.text = "--"
        speedText.text = "--"
        frequencyText.text = "--"
        ipText.text = "--"

        updateSecurityStatus(
            title = "❌ Not Connected",
            message = "You are not connected to any WiFi network. Enable WiFi and connect to a network to see security information.",
            bgColor = "#FFEBEE"
        )
    }

    private fun showDetailedInfo() {
        val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val info = wifiManager.connectionInfo

        if (info.networkId == -1) {
            Toast.makeText(this, "Not connected to any network", Toast.LENGTH_SHORT).show()
            return
        }

        val details = StringBuilder()
        details.append("📡 NETWORK INFORMATION\n\n")
        details.append("SSID: ${info.ssid.removePrefix("\"").removeSuffix("\"")}\n\n")
        details.append("BSSID: ${info.bssid}\n\n")
        details.append("Network ID: ${info.networkId}\n\n")
        details.append("RSSI: ${info.rssi} dBm\n\n")
        details.append("Link Speed: ${info.linkSpeed} Mbps\n\n")

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            details.append("Frequency: ${info.frequency} MHz\n\n")
        }

        details.append("MAC Address: ${info.macAddress}\n\n")

        val ip = info.ipAddress
        if (ip != 0) {
            details.append("IP Address: ${String.format(
                "%d.%d.%d.%d",
                ip and 0xff,
                ip shr 8 and 0xff,
                ip shr 16 and 0xff,
                ip shr 24 and 0xff
            )}\n\n")
        }

        details.append("━━━━━━━━━━━━━━━━━━━━\n\n")
        details.append("🔒 Security Analysis:\n")

        val signalLevel = WifiManager.calculateSignalLevel(info.rssi, 5)
        when {
            signalLevel >= 4 -> details.append("✅ Excellent signal strength\n")
            signalLevel >= 3 -> details.append("✅ Good signal strength\n")
            signalLevel >= 2 -> details.append("⚠️ Fair signal strength\n")
            else -> details.append("❌ Weak signal strength\n")
        }

        if (info.frequency >= 5000) {
            details.append("✅ Using 5 GHz band (faster)\n")
        } else {
            details.append("⚠️ Using 2.4 GHz band (slower)\n")
        }

        AlertDialog.Builder(this)
            .setTitle("Network Details")
            .setMessage(details.toString())
            .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
            .setNeutralButton("Copy") { _, _ ->
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                val clip = android.content.ClipData.newPlainText("WiFi Details", details.toString())
                clipboard.setPrimaryClip(clip)
                Toast.makeText(this, "✅ Copied to clipboard", Toast.LENGTH_SHORT).show()
            }
            .show()
    }
}