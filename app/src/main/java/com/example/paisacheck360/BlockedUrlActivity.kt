package com.example.paisacheck360

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class BlockedUrlActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_blocked_url)

        val txtUrl: TextView = findViewById(R.id.txtUrl)
        val txtReasons: TextView = findViewById(R.id.txtReasons)
        val btnOpenAnyway: Button = findViewById(R.id.btnOpenAnyway)
        val btnClose: Button = findViewById(R.id.btnClose)

        val url = intent.getStringExtra("url") ?: "Unknown URL"
        val reasons = intent.getStringArrayListExtra("reasons") ?: arrayListOf("No reasons available")

        txtUrl.text = url
        txtReasons.text = reasons.joinToString("\n- ", prefix = "- ")

        // ❌ Make text red
        txtReasons.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark))

        // OPEN ANYWAY (USER RISK)
        btnOpenAnyway.setOnClickListener {
            val i = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(i)
            finish()
        }

        // CLOSE BUTTON
        btnClose.setOnClickListener {
            finish()
        }
    }
}
