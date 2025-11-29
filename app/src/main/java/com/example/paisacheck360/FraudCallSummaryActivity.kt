package com.example.paisacheck360

import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.*

class FraudCallSummaryActivity : AppCompatActivity() {

    private lateinit var callListContainer: LinearLayout
    private lateinit var db: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fraud_call_summary)

        callListContainer = findViewById(R.id.callListContainer)

        // 1. Get Android ID to find the user's data
        val androidID = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID) ?: "guest"

        // 2. Point to the correct path in Firebase
        db = FirebaseDatabase.getInstance("https://sbtest-9acea-default-rtdb.firebaseio.com")
            .reference.child("users").child(androidID).child("call_feedback")

        loadRealData()
    }

    private fun loadRealData() {
        db.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                callListContainer.removeAllViews()

                if (!snapshot.exists()) {
                    Toast.makeText(this@FraudCallSummaryActivity, "No call logs found.", Toast.LENGTH_SHORT).show()
                    return
                }

                // Loop through all saved calls
                for (child in snapshot.children) {
                    // The key is the phone number (e.g., +918652811987)
                    val phoneNumber = child.key ?: "Unknown"
                    val status = child.child("status").getValue(String::class.java) ?: "Unknown"
                    val timestamp = child.child("timestamp").getValue(String::class.java) ?: ""

                    addCallView(phoneNumber, status, timestamp)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FraudCallSummary", "Failed to load logs: ${error.message}")
            }
        })
    }

    private fun addCallView(number: String, status: String, date: String) {
        val inflater = LayoutInflater.from(this)
        // Using simple_list_item_2 is fine, or you can make a custom layout later
        val itemView = inflater.inflate(android.R.layout.simple_list_item_2, callListContainer, false)

        val text1 = itemView.findViewById<TextView>(android.R.id.text1)
        val text2 = itemView.findViewById<TextView>(android.R.id.text2)

        // Format the text
        text1.text = "📞 $number"
        text2.text = "Status: $status  •  Time: $date"

        // Optional: Color code the scam calls
        if (status == "Scam") {
            text1.setTextColor(Color.RED)
        } else {
            text1.setTextColor(Color.parseColor("#2E7D32")) // Dark Green
        }

        callListContainer.addView(itemView)
    }
}