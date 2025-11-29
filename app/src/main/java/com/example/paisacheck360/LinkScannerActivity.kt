package com.example.paisacheck360

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class LinkScannerActivity : AppCompatActivity() {

    private lateinit var editUrl: EditText
    private lateinit var btnScan: Button
    private lateinit var txtResult: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.link_scanner)

        editUrl = findViewById(R.id.editUrl)
        btnScan = findViewById(R.id.btnScan)
        txtResult = findViewById(R.id.txtResult)

        // ⭐ If opened from notification
        val incomingUrl = intent.getStringExtra("scanned_url")
        val incomingReasons = intent.getStringArrayListExtra("scanned_reasons")

        if (!incomingUrl.isNullOrBlank()) {
            editUrl.setText(incomingUrl)

            val result = UrlAnalyzer.analyzeUrl(incomingUrl)
            displayResult(result)

            if (!incomingReasons.isNullOrEmpty()) {
                txtResult.append("\n\n📋 More reasons:\n- ${incomingReasons.joinToString("\n- ")}")
            }
        }

        // ⭐ Scan button
        btnScan.setOnClickListener {
            val url = editUrl.text.toString().trim()

            if (url.isEmpty()) {
                Toast.makeText(this, "Please enter a URL", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            txtResult.text = "🔍 Scanning..."

            txtResult.post {
                val result = UrlAnalyzer.analyzeUrl(url)
                displayResult(result)

                // ⭐ Secure open logic
                UrlBlocker.openUrlWithProtection(this, url)
            }
        }
    }

    override fun onResume() {
        super.onResume()

        // 📋 Auto-fill clipboard URL
        try {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip: ClipData? = clipboard.primaryClip
            val clipText = clip?.getItemAt(0)?.text?.toString()

            if (!clipText.isNullOrBlank()) {
                if (UrlAnalyzer.extractUrls(clipText).isNotEmpty()) {
                    if (editUrl.text.toString().isBlank()) {
                        editUrl.setText(clipText.trim())
                        txtResult.text = "📋 URL pasted from clipboard - tap Scan"
                    }
                }
            }
        } catch (_: Exception) { }
    }

    /** ⭐ Display result UI */
    private fun displayResult(result: UrlAnalyzer.AnalysisResult) {
        val colorRes = when (result.level) {
            UrlAnalyzer.RiskLevel.SAFE -> android.R.color.holo_green_dark
            UrlAnalyzer.RiskLevel.SUSPICIOUS -> android.R.color.holo_orange_dark
            UrlAnalyzer.RiskLevel.DANGEROUS -> android.R.color.holo_red_dark
        }

        val emoji = when (result.level) {
            UrlAnalyzer.RiskLevel.SAFE -> "✅"
            UrlAnalyzer.RiskLevel.SUSPICIOUS -> "⚠️"
            UrlAnalyzer.RiskLevel.DANGEROUS -> "🚫"
        }

        val baseText = """
            $emoji ${result.level.name} link
            
            Reasons:
            - ${result.reasons.joinToString("\n- ")}
        """.trimIndent()

        txtResult.setTextColor(ContextCompat.getColor(this, colorRes))
        txtResult.text = baseText
    }
}
