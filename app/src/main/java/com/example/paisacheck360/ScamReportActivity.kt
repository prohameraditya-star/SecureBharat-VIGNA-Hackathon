package com.example.paisacheck360

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class ScamReportActivity : AppCompatActivity() {

    private lateinit var db: DatabaseReference
    private lateinit var recyclerScamLogs: RecyclerView
    private lateinit var totalScams: TextView
    private lateinit var highRisk: TextView
    private lateinit var blocked: TextView
    private lateinit var backButton: ImageButton

    private lateinit var adapter: ScamAdapter
    private val scamList = mutableListOf<ScamData>()
    private var blockedCount = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scam_report)

        recyclerScamLogs = findViewById(R.id.recyclerScamLogs)
        totalScams = findViewById(R.id.totalScamsCount)
        highRisk = findViewById(R.id.highRiskCount)
        blocked = findViewById(R.id.blockedCount)
        backButton = findViewById(R.id.backButton)

        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: "guest"
        db = FirebaseDatabase.getInstance().getReference("users/$uid/alerts")

        recyclerScamLogs.layoutManager = LinearLayoutManager(this)
        adapter = ScamAdapter(scamList,
            onBlock = { scam ->
                blockedCount++
                blocked.text = blockedCount.toString()
                Toast.makeText(this, "🚫 Sender blocked: ${scam.sender}", Toast.LENGTH_SHORT).show()
            },
            onReport = { scam ->
                Toast.makeText(this, "🚨 Reported to Cyber Cell: ${scam.sender}", Toast.LENGTH_SHORT).show()
            }
        )
        recyclerScamLogs.adapter = adapter

        loadReportData()

        backButton.setOnClickListener { finish() }

        findViewById<Button>(R.id.exportPdfBtn).setOnClickListener {
            Toast.makeText(this, "📄 Export to PDF coming soon!", Toast.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.shareReportBtn).setOnClickListener {
            Toast.makeText(this, "📱 Share option coming soon!", Toast.LENGTH_SHORT).show()
        }

        // 🔥 Filter Buttons
        findViewById<Button>(R.id.filterAll).setOnClickListener { adapter.filterByRisk("All") }
        findViewById<Button>(R.id.filterHigh).setOnClickListener { adapter.filterByRisk("High") }
        findViewById<Button>(R.id.filterMedium).setOnClickListener { adapter.filterByRisk("Medium") }
        findViewById<Button>(R.id.filterLow).setOnClickListener { adapter.filterByRisk("Low") }
    }

    private fun loadReportData() {
        db.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                scamList.clear()
                var total = 0
                var high = 0

                for (child in snapshot.children) {
                    val scam = child.getValue(ScamData::class.java)
                    scam?.let {
                        scamList.add(it)
                        if (it.risk != "Safe") total++
                        if (it.risk == "High" || it.risk == "Critical") high++
                    }
                }

                // Sort recent first
                scamList.sortByDescending { it.timestamp }

                totalScams.text = total.toString()
                highRisk.text = high.toString()

                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@ScamReportActivity, "Error loading report", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
