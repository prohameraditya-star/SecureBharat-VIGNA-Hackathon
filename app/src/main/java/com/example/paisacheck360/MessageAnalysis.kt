package com.example.paisacheck360

/**
 * A data class to hold the results of the SMS analysis.
 */
data class MessageAnalysis(
    val intent: String,
    val type: String, // Used to categorize the message (e.g., "Transaction", "Promo")
    val tone: String, // e.g., "Urgent", "Neutral"
    val risk: String, // e.g., "High", "Safe"
    val suggestedReplies: List<String>
)