package com.example.paisacheck360

enum class RiskLevel(val displayName: String, val color: Int, val emoji: String) {
    LOW("Low Risk", 0xFF4CAF50.toInt(), "🟢"),
    MEDIUM("Medium Risk", 0xFFFFC107.toInt(), "🟡"),
    HIGH("High Risk", 0xFFF44336.toInt(), "🔴"),
    CRITICAL("Critical Risk", 0xFF9C27B0.toInt(), "⚠️")
}