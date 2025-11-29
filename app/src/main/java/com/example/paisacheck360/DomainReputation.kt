package com.example.paisacheck360

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetAddress
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

object DomainReputation {
    private val TAG = "DomainReputation"

    // Local blacklist (short). Expand as needed or load from Firebase threatfeed later.
    private val localBlacklist = setOf(
        "free-loan-apps.example", "cheaploan.payfast", "badloan.xyz"
        // Add real known scam domains during testing
    )

    // Simple cache for domain checks
    private val cache = ConcurrentHashMap<String, Boolean>()

    fun isDomainSuspicious(domain: String): Boolean {
        // Quick local check
        if (localBlacklist.any { domain.contains(it) }) return true

        // Check cached result
        cache[domain]?.let { return it }

        // Heuristic checks: TLD short ones, suspicious keywords
        val suspiciousTld = listOf(".tk", ".ml", ".ga", ".cf", ".gq")
        if (suspiciousTld.any { domain.endsWith(it) }) {
            cache[domain] = true
            return true
        }

        // simple keyword heuristics
        val suspiciousKeywords = listOf("loan", "free", "cash", "reward", "win", "prize")
        if (suspiciousKeywords.any { domain.contains(it) }) {
            cache[domain] = true
            return true
        }

        // Not obviously suspicious locally -> consider safe for quick path
        cache[domain] = false
        return false
    }

    // Optional async cloud check (example stub)
    suspend fun checkDomainRemotely(domain: String): Boolean = withContext(Dispatchers.IO) {
        try {
            // Example: You could call a safe-browsing API here.
            // This is a placeholder that just returns false (not suspicious).
            // If you want integration, implement here (careful with API keys).
            false
        } catch (e: Exception) {
            Log.e(TAG, "Remote check failed: ${e.message}")
            false
        }
    }
}
