package com.example.paisacheck360

import android.Manifest

object PermissionAnalyzer {

    // ⚠️ High-risk permissions with scoring
    private val DANGEROUS_PERMISSIONS = mapOf(
        // 🔴 Critical (15 points each)
        Manifest.permission.READ_SMS to 15,
        Manifest.permission.RECEIVE_SMS to 15,
        Manifest.permission.SEND_SMS to 15,
        Manifest.permission.READ_CALL_LOG to 15,
        Manifest.permission.WRITE_CALL_LOG to 15,
        Manifest.permission.CALL_PHONE to 12,
        Manifest.permission.READ_CONTACTS to 12,
        Manifest.permission.WRITE_CONTACTS to 12,
        Manifest.permission.ACCESS_FINE_LOCATION to 10,
        Manifest.permission.ACCESS_COARSE_LOCATION to 8,
        Manifest.permission.CAMERA to 10,
        Manifest.permission.RECORD_AUDIO to 10,
        Manifest.permission.READ_EXTERNAL_STORAGE to 8,
        Manifest.permission.WRITE_EXTERNAL_STORAGE to 8,
        Manifest.permission.GET_ACCOUNTS to 12,
        Manifest.permission.READ_PHONE_STATE to 10,
        Manifest.permission.PROCESS_OUTGOING_CALLS to 12,
        Manifest.permission.SYSTEM_ALERT_WINDOW to 10,
        Manifest.permission.REQUEST_INSTALL_PACKAGES to 15,
        Manifest.permission.WRITE_SETTINGS to 8,
        Manifest.permission.BIND_NOTIFICATION_LISTENER_SERVICE to 12,
        Manifest.permission.BIND_ACCESSIBILITY_SERVICE to 15
    )

    /**
     * Analyzes permissions and returns risk score (0-100)
     */
    fun analyzePermissions(permissions: List<String>): Pair<Int, List<String>> {
        var score = 0
        val dangerousFound = mutableListOf<String>()

        permissions.forEach { permission ->
            DANGEROUS_PERMISSIONS[permission]?.let { points ->
                score += points
                dangerousFound.add(getPermissionDisplayName(permission))
            }
        }

        // Cap score at 100
        return Pair(minOf(score, 100), dangerousFound)
    }

    /**
     * Determines risk level based on score
     */
    fun getRiskLevel(score: Int): RiskLevel {
        return when {
            score >= 60 -> RiskLevel.CRITICAL
            score >= 40 -> RiskLevel.HIGH
            score >= 20 -> RiskLevel.MEDIUM
            else -> RiskLevel.LOW
        }
    }

    /**
     * Converts permission constant to readable name
     */
    private fun getPermissionDisplayName(permission: String): String {
        return when (permission) {
            Manifest.permission.READ_SMS -> "📧 Read SMS"
            Manifest.permission.RECEIVE_SMS -> "📥 Receive SMS"
            Manifest.permission.SEND_SMS -> "📤 Send SMS"
            Manifest.permission.READ_CALL_LOG -> "📞 Read Call Log"
            Manifest.permission.WRITE_CALL_LOG -> "📝 Write Call Log"
            Manifest.permission.CALL_PHONE -> "☎️ Make Calls"
            Manifest.permission.READ_CONTACTS -> "👥 Read Contacts"
            Manifest.permission.WRITE_CONTACTS -> "✍️ Write Contacts"
            Manifest.permission.ACCESS_FINE_LOCATION -> "📍 Precise Location"
            Manifest.permission.ACCESS_COARSE_LOCATION -> "🗺️ Approximate Location"
            Manifest.permission.CAMERA -> "📷 Camera"
            Manifest.permission.RECORD_AUDIO -> "🎤 Microphone"
            Manifest.permission.READ_EXTERNAL_STORAGE -> "💾 Read Storage"
            Manifest.permission.WRITE_EXTERNAL_STORAGE -> "💿 Write Storage"
            Manifest.permission.GET_ACCOUNTS -> "🔐 Access Accounts"
            Manifest.permission.READ_PHONE_STATE -> "📱 Phone State"
            Manifest.permission.SYSTEM_ALERT_WINDOW -> "🪟 Overlay Apps"
            Manifest.permission.REQUEST_INSTALL_PACKAGES -> "📦 Install Apps"
            Manifest.permission.BIND_ACCESSIBILITY_SERVICE -> "♿ Accessibility Service"
            else -> permission.substringAfterLast(".")
        }
    }
}