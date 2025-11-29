package com.example.paisacheck360

import android.app.Activity
import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class VpnPermissionActivity : AppCompatActivity() {

    private val REQUEST_VPN_PERMISSION = 0xF10

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Simple UI-less flow: request VPN permission immediately
        requestVpnPermission()
    }

    private fun requestVpnPermission() {
        val intent = VpnService.prepare(this)
        if (intent != null) {
            // Launch system dialog to grant VPN permission
            startActivityForResult(intent, REQUEST_VPN_PERMISSION)
        } else {
            // already granted
            startVpnService()
        }
    }

    private fun startVpnService() {
        try {
            val mode = getSharedPreferences("SecureBharatPrefs", MODE_PRIVATE)
                .getString("vpn_mode", "monitor") ?: "monitor"

            val svc = Intent(this, LocalVpnService::class.java).apply {
                action = "START"
                putExtra("mode", mode)
            }
            startService(svc)
            Toast.makeText(this, "SecureBharat VPN started ($mode)", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e("VpnPermissionActivity", "Failed to start VPN: ${e.message}")
        } finally {
            finish()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_VPN_PERMISSION) {
            if (resultCode == Activity.RESULT_OK) {
                startVpnService()
            } else {
                Toast.makeText(this, "VPN permission denied", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
}
