package com.example.paisacheck360

import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity

class SecureBrowserActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_secure_browser)

        val webView = findViewById<WebView>(R.id.safeWebView)
        val url = intent.getStringExtra("url") ?: "https://google.com"

        // Safe browser settings
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true

        webView.webViewClient = object : WebViewClient() {}

        webView.loadUrl(url)
    }
}
