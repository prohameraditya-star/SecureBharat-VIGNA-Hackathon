package com.example.paisacheck360

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import kotlinx.coroutines.*
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.InetAddress
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.DatagramChannel
import kotlin.experimental.and

/**
 * LocalVpnService
 *
 * Lightweight VPN that intercepts DNS (UDP/53) requests.
 * Modes:
 *  - monitorOnly = true -> log suspicious domains
 *  - monitorOnly = false -> block suspicious domains (drop or NXDOMAIN)
 *
 * NOTE: This is a conservative skeleton. It captures DNS packets at TUN interface
 *       and forwards queries via a local UDP socket to an upstream (optional).
 *       We avoid building a full TCP/HTTP proxy here.
 */
class LocalVpnService : VpnService() {

    private val TAG = "LocalVpnService"
    private var vpnInterface: ParcelFileDescriptor? = null
    private var job: Job? = null

    companion object {
        const val PREFS_VPN_MODE = "vpn_mode" // "monitor" or "block"
        const val ONGOING_NOTIFICATION_ID = 9988
        const val CHANNEL_ID = "secure_bharat_vpn_channel"
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        Log.d(TAG, "onStartCommand action=$action")
        when (action) {
            "START" -> startVpn()
            "STOP" -> stopVpn()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        stopVpn()
        super.onDestroy()
    }

    private fun ensureNotification() {
        // Foreground service notification required on modern Android
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "SecureBharat VPN", NotificationManager.IMPORTANCE_LOW)
            nm.createNotificationChannel(channel)
        }
        val notification = Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("Secure Bharat – Network Shield")
            .setContentText("Monitoring network connections")
            .setSmallIcon(R.mipmap.ic_launcher)
            .build()
        startForeground(ONGOING_NOTIFICATION_ID, notification)
    }

    fun startVpn() {
        if (vpnInterface != null) {
            Log.d(TAG, "VPN already running")
            return
        }

        // Prepare VPN (show consent dialog if needed)
        val intent = VpnService.prepare(this)
        if (intent != null) {
            // Caller must startActivityForResult with this intent from an Activity to get user consent.
            Log.w(TAG, "VpnService.prepare() returned an intent — start activity for result")
            val launch = Intent(this, VpnPermissionActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(launch)
            return
        }

        ensureNotification()

        // Build VPN interface
        val builder = Builder()
        builder.setSession("SecureBharat-NetworkShield")
        // Add a dummy address on the TUN interface (not used for routing to remote)
        builder.addAddress("10.0.0.2", 32)
        // Catch all IPv4 traffic
        builder.addRoute("0.0.0.0", 0)
        // Set DNS to Google fallback (will be used if we forward), but actual interception occurs on TUN
        builder.addDnsServer("8.8.8.8")
        builder.setBlocking(true)

        vpnInterface = builder.establish()

        vpnInterface?.let { pfd ->
            job = CoroutineScope(Dispatchers.IO).launch {
                try {
                    Log.d(TAG, "VPN established, starting packet worker")
                    packetWorker(pfd)
                } catch (e: Exception) {
                    Log.e(TAG, "packetWorker failed: ${e.message}", e)
                } finally {
                    Log.d(TAG, "packetWorker exiting")
                }
            }
        }
    }

    fun stopVpn() {
        Log.d(TAG, "Stopping VPN")
        job?.cancel()
        job = null
        vpnInterface?.close()
        vpnInterface = null
        stopForeground(true)
        stopSelf()
    }

    /**
     * Packet worker reads raw IP packets from the TUN interface.
     * We parse the IPv4 header to find UDP packets to port 53 (DNS).
     * When a DNS query is detected, extract the queried domain and handle it.
     */
    private suspend fun packetWorker(pfd: ParcelFileDescriptor) = withContext(Dispatchers.IO) {
        val input = FileInputStream(pfd.fileDescriptor)
        val output = FileOutputStream(pfd.fileDescriptor)
        val buffer = ByteArray(32767)

        while (isActive) {
            val read = input.read(buffer)
            if (read <= 0) {
                delay(10)
                continue
            }

            // Parse IPv4 header (simple, no fragmentation handling)
            if (read >= 20) {
                val packet = ByteBuffer.wrap(buffer, 0, read)

                val versionAndIHL = packet.get(0)
                val version = ((versionAndIHL.toInt() shr 4) and 0x0F)
                if (version != 4) {
                    // Non-IPv4: ignore for now
                    continue
                }

                val ihl = (versionAndIHL.toInt() and 0x0F) * 4
                val protocol = packet.get(9).toInt() and 0xFF

                // Only handle UDP (protocol 17)
                if (protocol == 17) {
                    // UDP source and dest ports are after IP header
                    val udpHeaderStart = ihl
                    if (read >= udpHeaderStart + 8) {
                        val srcPort = ((packet.getShort(udpHeaderStart).toInt() and 0xFFFF))
                        val dstPort = ((packet.getShort(udpHeaderStart + 2).toInt() and 0xFFFF))

                        // If destination port is 53 (DNS), parse DNS query
                        if (dstPort == 53 || srcPort == 53) {
                            val dnsPayloadStart = udpHeaderStart + 8
                            val dnsLen = read - dnsPayloadStart
                            if (dnsLen > 12) {
                                val domain = parseDnsQueryDomain(buffer, dnsPayloadStart, dnsLen)
                                if (domain != null) {
                                    handleDnsQuery(domain, appFromPacket(buffer))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Minimal DNS question parser: returns the queried domain or null.
     */
    private fun parseDnsQueryDomain(data: ByteArray, offset: Int, len: Int): String? {
        try {
            var idx = offset + 12 // DNS header is 12 bytes; question section follows
            val end = offset + len
            val sb = StringBuilder()
            while (idx < end) {
                val labelLen = data[idx].toInt() and 0xFF
                if (labelLen == 0) break
                if (sb.isNotEmpty()) sb.append('.')
                val start = idx + 1
                val label = String(data, start, labelLen)
                sb.append(label)
                idx = start + labelLen
            }
            val domain = sb.toString()
            if (domain.isBlank()) return null
            return domain.lowercase()
        } catch (e: Exception) {
            return null
        }
    }

    /**
     * Attempt to identify the app from packet metadata.
     * Low-fidelity: not guaranteed. For demo we return null.
     * Advanced approaches: map UID from /proc/net/udp entries or use getConnectionOwnerUid (requires platform APIs).
     */
    private fun appFromPacket(packet: ByteArray): String? {
        // Place-holder: implementing accurate UID mapping requires system-level access.
        // We'll return null here; later we can integrate per-app mapping by reading /proc/net/udp or using VpnService.Builder.setMetered()
        return null
    }

    /**
     * Handle DNS queries (monitor / block).
     */
    private fun handleDnsQuery(domain: String, appPackage: String?) {
        // Check domain reputation (synchronous quick check)
        val isSuspicious = DomainReputation.isDomainSuspicious(domain)

        // Read mode
        val prefs = getSharedPreferences("SecureBharatPrefs", MODE_PRIVATE)
        val mode = prefs.getString("vpn_mode", "monitor") ?: "monitor"

        if (isSuspicious) {
            // Log and raise app risk (if appPackage known)
            Log.w(TAG, "Suspicious domain detected: $domain (app=$appPackage)")

            // Increase app risk score in local DB / SharedPreferences or broadcast an event
            // Here we broadcast a local intent so UI / AppScanner can catch and update scores
            val intent = Intent("com.securebharat.NETWORK_THREAT").apply {
                putExtra("domain", domain)
                putExtra("suspicious", true)
                putExtra("appPackage", appPackage)
            }
            sendBroadcast(intent)

            if (mode == "block") {
                // In block mode, we want to "drop" this DNS query or cause resolution failure.
                // Simplest demo: just log and do nothing (the app will likely timeout).
                // Advanced: respond with NXDOMAIN by crafting a DNS response packet and writing back to TUN.
                Log.w(TAG, "Mode=block → blocking domain: $domain")
                // (Implement NXDOMAIN response if needed — complex, omitted in skeleton)
            }
        } else {
            // Not suspicious: optionally log
            Log.d(TAG, "DNS OK: $domain")
        }
    }
}
