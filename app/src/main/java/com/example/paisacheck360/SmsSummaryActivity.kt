package com.example.paisacheck360

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class SmsSummaryActivity : AppCompatActivity() {

    private lateinit var smsListContainer: LinearLayout
    private lateinit var etSearch: EditText
    private val gson = Gson()
    private var allScams: List<ScamMessage> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // XML you shared earlier
        setContentView(R.layout.activity_sms_detector)

        smsListContainer = findViewById(R.id.smsListContainer)
        etSearch = findViewById(R.id.etSearch)

        // 1) stats from MainActivity
        val scannedCount = intent.getIntExtra("scannedCount", 0)
        val flaggedCount = intent.getIntExtra("flaggedCount", 0)
        val upiCount = intent.getIntExtra("upiCount", 0)
        findViewById<TextView>(R.id.tvStats).text =
            "$scannedCount SMS scanned | $flaggedCount scams flagged | $upiCount UPI frauds"

        // 2) load from SharedPreferences (single key: "scam_logs")
        val prefs = getSharedPreferences("SecureBharatPrefs", MODE_PRIVATE)
        val json = prefs.getString("scam_logs", "[]")
        val type = object : TypeToken<List<ScamMessage>>() {}.type
        allScams = gson.fromJson(json, type) ?: emptyList()

        // 3) show & wire search filter
        showScamMessages(allScams)

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val q = s?.toString()?.trim()?.lowercase().orEmpty()
                val filtered = if (q.isEmpty()) allScams else allScams.filter {
                    it.sender.lowercase().contains(q) ||
                            it.text.lowercase().contains(q) ||
                            it.riskLevel.lowercase().contains(q)
                }
                showScamMessages(filtered)
            }
        })
    }

    private fun showScamMessages(list: List<ScamMessage>) {
        smsListContainer.removeAllViews()
        val inflater = LayoutInflater.from(this)

        if (list.isEmpty()) {
            val emptyText = TextView(this).apply {
                text = "No suspicious SMS detected yet 🚫"
                textSize = 16f
                setPadding(16, 16, 16, 16)
            }
            smsListContainer.addView(emptyText)
            return
        }

        list.forEach { scam ->
            val itemView = inflater.inflate(android.R.layout.simple_list_item_2, smsListContainer, false)
            val text1 = itemView.findViewById<TextView>(android.R.id.text1)
            val text2 = itemView.findViewById<TextView>(android.R.id.text2)

            text1.text = "${scam.sender} • ${scam.date}"
            text2.text = "${scam.text} | Risk: ${scam.riskLevel}"

            // Optional: simple color hint by risk
            when (scam.riskLevel.lowercase()) {
                "high" -> text2.setTextColor(0xFFB00020.toInt())   // red-ish
                "medium" -> text2.setTextColor(0xFFFF8F00.toInt()) // orange-ish
                else -> text2.setTextColor(0xFF2E7D32.toInt())     // green-ish
            }

            smsListContainer.addView(itemView)
        }
    }
}
