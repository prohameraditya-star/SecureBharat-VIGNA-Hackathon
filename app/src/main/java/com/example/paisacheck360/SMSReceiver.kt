package com.example.paisacheck360

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Telephony
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.util.ArrayList
import java.util.Locale
import java.util.regex.Pattern

class SMSReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (Telephony.Sms.Intents.SMS_RECEIVED_ACTION == intent.action) {
            val msgs = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            for (msg in msgs) {
                val sender = msg.displayOriginatingAddress ?: "Unknown"
                val body = msg.displayMessageBody ?: ""
                Log.d("SMSReceiver", "📩 From: $sender | $body")

                val analysis = analyzeMessage(sender, body)

                // Save to Firebase
                saveToFirebase(sender, body, analysis)

                // Start the popup service
                startPopup(context, sender, body, analysis)
            }
        }
    }

    private fun startPopup(context: Context, sender: String, body: String, analysis: MessageAnalysis) {
        val i = Intent(context, ScamPopupService::class.java).apply {
            putExtra("sender", sender)
            putExtra("body", body)
            putExtra("intent", analysis.intent)
            putExtra("tone", analysis.tone)
            putExtra("risk", analysis.risk)
            putStringArrayListExtra("suggestedReplies", ArrayList(analysis.suggestedReplies))
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(i)
            } else {
                context.startService(i)
            }
        } catch (e: Exception) {
            Log.e("SMSReceiver", "Failed to start popup service: ${e.message}")
        }
    }

    private fun saveToFirebase(sender: String, body: String, analysis: MessageAnalysis) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: "guest"
        val db = FirebaseDatabase.getInstance().getReference("users/$uid/alerts")
        val id = db.push().key ?: System.currentTimeMillis().toString()
        val payload = mapOf(
            "sender" to sender,
            "body" to body,
            "intent" to analysis.intent,
            "risk" to analysis.risk,
            "timestamp" to System.currentTimeMillis()
        )
        db.child(id).setValue(payload)
    }

    private fun analyzeMessage(sender: String, message: String): MessageAnalysis {
        val m = message.lowercase(Locale.getDefault()).trim()

        // Regex Patterns
        val urlPattern = Pattern.compile("""https?://\S+|\bbit\.ly/\S+|\bgoo\.gl/\S+""")
        val moneyPattern = Pattern.compile("""\b(rs\.?|inr|₹)\s?[\d,]+""")
        val otpPattern = Pattern.compile("""\b(\d{4,8})\b""")

        // ❗️ EXPANDED KEYWORD LISTS
        val scamWords = listOf(
            "win", "winner", "won", "winning",
            "lottery", "prize", "reward", "award",
            "congratulations", "congrats",
            "claim", "claim now",
            "urgent", "immediately", "action required", "blocked", "suspended",
            "kyc", "pan card", "update now", "verify",
            "free", "offer", "gift", "bonus", "cash", "credit", "loan"
        )

        val bankingWords = listOf("debited", "credited", "txn", "acct", "account", "bank", "otp", "spent", "payment")

        // Logic
        var intent = "UNKNOWN"
        var risk = "Safe"
        var tone = "neutral"

        // 1. Check for SCAM keywords first
        if (scamWords.any { m.contains(it) } || (m.contains("click") && urlPattern.matcher(m).find())) {
            intent = "POTENTIAL_SCAM"
            risk = "High"
            tone = "urgent"
        }
        // 2. Check for Banking/Transaction
        else if (bankingWords.any { m.contains(it) } || moneyPattern.matcher(m).find()) {
            intent = "TRANSACTION"
            risk = "Low"
            // If a bank message has a link, it might be phishing
            if (urlPattern.matcher(m).find()) {
                risk = "Medium"
                intent = "SUSPICIOUS_LINK"
            }
        }
        // 3. Check for OTP
        else if (m.contains("otp") || otpPattern.matcher(m).find()) {
            intent = "OTP"
            risk = "Low"
        }

        return MessageAnalysis(intent, intent, tone, risk, arrayListOf())
    }
}