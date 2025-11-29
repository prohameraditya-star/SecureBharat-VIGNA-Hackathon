package com.example.paisacheck360

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log

object UrlBlocker {

    // Store last analyzed URL results
    private val dangerMap = mutableMapOf<String, UrlAnalyzer.AnalysisResult>()

    /**
     * Called from SMSReceiver:
     * Stores analyzed URL so we can block it later when user clicks it.
     */
    fun registerIncomingUrl(result: UrlAnalyzer.AnalysisResult) {
        dangerMap[result.url] = result
        Log.d("UrlBlocker", "Registered URL for protection: ${result.url}")
    }

    /**
     * Intercepts URL open request.
     * If URL is dangerous → block, else open normally.
     */
    fun openUrlWithProtection(context: Context, url: String) {
        val result = dangerMap[url] ?: UrlAnalyzer.analyzeUrl(url)

        when (result.level) {
            UrlAnalyzer.RiskLevel.DANGEROUS -> {
                val i = Intent(context, BlockedUrlActivity::class.java).apply {
                    putExtra("url", url)
                    putStringArrayListExtra("reasons", ArrayList(result.reasons))
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(i)
            }

            UrlAnalyzer.RiskLevel.SUSPICIOUS -> {
                val i = Intent(context, BlockedUrlActivity::class.java).apply {
                    putExtra("url", url)
                    putStringArrayListExtra("reasons", ArrayList(result.reasons))
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(i)
            }

            UrlAnalyzer.RiskLevel.SAFE -> {
                val i = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(i)
            }
        }
    }
}
