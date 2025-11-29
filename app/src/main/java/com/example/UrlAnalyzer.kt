package com.example.paisacheck360

object UrlAnalyzer {

    enum class RiskLevel { SAFE, SUSPICIOUS, DANGEROUS }

    data class AnalysisResult(
        val url: String,
        val score: Int,
        val level: RiskLevel,
        val reasons: List<String>
    )

    // Regex for detecting URLs
    private val urlRegex = Regex("(https?://[\\w.-]+(?:\\.[\\w\\.-]+)+[/\\w\\-?=%.&]*)",
        RegexOption.IGNORE_CASE
    )

    // Scam keywords common in India
    private val indianScamWords = listOf(
        "kyc", "update", "verify", "blocked", "reactivate",
        "account", "bank", "sbi", "axis", "hdfc", "icici",
        "upi", "customs", "parcel", "reward", "prize",
        "bonus", "gift", "amazon", "flipkart"
    )

    private val shorteners = listOf(
        "bit.ly", "tinyurl", "t.co", "is.gd", "cutt.ly", "shorturl"
    )

    private val dangerousTlds = listOf(
        ".xyz", ".top", ".buzz", ".monster", ".online", ".site"
    )

    fun extractUrls(text: String): List<String> {
        return urlRegex.findAll(text).map { it.value }.toList()
    }

    fun analyzeUrl(urlInput: String): AnalysisResult {
        val url = urlInput.trim().lowercase()
        var score = 0
        val reasons = mutableListOf<String>()

        // Not a valid URL
        if (!urlRegex.containsMatchIn(url)) {
            return AnalysisResult(url, 0, RiskLevel.SAFE, listOf("Not a valid URL"))
        }

        // Rule 1 — shortened URLs
        if (shorteners.any { url.contains(it) }) {
            score += 3
            reasons += "Uses URL shortener (high risk)"
        }

        // Rule 2 — keywords used by scammers in India
        val matchedWords = indianScamWords.filter { url.contains(it) }
        if (matchedWords.isNotEmpty()) {
            score += 2
            reasons += "Contains scam-related keywords: ${matchedWords.joinToString()}"
        }

        // Rule 3 — IP in URL
        if (Regex("https?://\\d+\\.\\d+\\.\\d+\\.\\d+").containsMatchIn(url)) {
            score += 3
            reasons += "URL uses raw IP address"
        }

        // Rule 4 — suspicious TLDs
        if (dangerousTlds.any { url.endsWith(it) }) {
            score += 2
            reasons += "Suspicious domain ending"
        }

        // Rule 5 — too many subdomains
        val dotCount = url.count { it == '.' }
        if (dotCount > 3) {
            score += 2
            reasons += "Too many subdomains (masking attempt)"
        }

        // Rule 6 — No HTTPS
        if (url.startsWith("http://")) {
            score += 1
            reasons += "Not using HTTPS"
        }

        // Rule 7 — Very long URL
        if (url.length > 120) {
            score += 1
            reasons += "Very long URL"
        }

        val level = when {
            score >= 6 -> RiskLevel.DANGEROUS
            score >= 3 -> RiskLevel.SUSPICIOUS
            else -> RiskLevel.SAFE
        }

        if (reasons.isEmpty()) reasons += "No suspicious patterns found"

        return AnalysisResult(url, score, level, reasons)
    }
}
