package com.example.paisacheck360

data class FraudMessage(
    val type: String,       // e.g., "SMS" or "UPI"
    val message: String,    // actual scam text
    val timestamp: Long     // when it was detected
)
