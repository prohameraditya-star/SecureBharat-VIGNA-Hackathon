package com.example.paisacheck360

// Simple data class to hold scam SMS info
data class ScamMessage(
    val sender: String,
    val text: String,
    val date: String,
    val riskLevel: String
)
