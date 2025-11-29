package com.example.paisacheck360

data class AppScanStatistics(
    val totalApps: Int,
    val criticalApps: Int,
    val highRiskApps: Int,
    val mediumRiskApps: Int,
    val lowRiskApps: Int
)
