package com.example.paisacheck360

import android.graphics.drawable.Drawable

data class AppRiskInfo(
    val appName: String,
    val packageName: String,
    val icon: Drawable?,
    val riskLevel: RiskLevel,
    val riskScore: Int,
    val dangerousPermissions: List<String>,
    val totalPermissions: Int,
    val installDate: Long,
    val lastUpdateDate: Long,
    val isSystemApp: Boolean,
    // 🔹 Phase-3: newly added risky permissions in latest state vs previous scan
    val newDangerousPermissions: List<String> = emptyList()
)
