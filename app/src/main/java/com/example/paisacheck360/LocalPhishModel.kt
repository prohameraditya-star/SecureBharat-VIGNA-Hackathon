package com.example.paisacheck360.network

import java.util.regex.Pattern

object LocalPhishModel {

    /**
     * Predict risk score for a scanned URL/text.
     * Score range: 0.0 (safe) → 1.0 (malicious).
     */
    fun predict(url: String): Float {
        return heuristicOnlyScore(url)
    }

    /**
     * Simple heuristic-based phishing detector.
     * Returns a score between 0.0 and 1.0.
     */
    fun heuristicOnlyScore(url: String): Float {
        val lower = url.lowercase()
        var hits = 0
        var checks = 0

        fun check(cond: Boolean) {
            checks++
            if (cond) hits++
        }

        // 🔴 Insecure protocol
        check(lower.startsWith("http://"))

        // 🔴 IP address in place of domain
        check(Pattern.compile("\\d+\\.\\d+\\.\\d+\\.\\d+").matcher(lower).find())

        // 🔴 Phishing keywords
        check(lower.contains("login") || lower.contains("verify") ||
                lower.contains("update") || lower.contains("secure"))

        // 🔴 Banking / payment keywords
        check(lower.contains("bank") || lower.contains("upi") ||
                lower.contains("gpay") || lower.contains("paytm") ||
                lower.contains("phonepe"))

        // 🔴 @ symbol in URL
        check(lower.contains("@"))

        // 🔴 Very long URL
        check(lower.length > 150)

        // 🔴 Too many subdomains
        check(lower.count { it == '.' } >= 4)

        // 🔴 URL shorteners
        check(lower.contains("bit.ly") || lower.contains("tinyurl") ||
                lower.contains("goo.gl") || lower.contains("t.co"))

        return if (checks > 0) hits.toFloat() / checks.toFloat() else 0f
    }

    /**
     * Human-readable classification from score.
     */
    fun classify(url: String): String {
        val score = predict(url)
        return when {
            score >= 0.75f -> "🚨 MALICIOUS"
            score >= 0.45f -> "⚠️ SUSPICIOUS"
            else -> "✅ SAFE"
        }
    }
}
