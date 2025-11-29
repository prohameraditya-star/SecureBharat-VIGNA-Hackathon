package com.example.paisacheck360

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.database.*
import com.example.paisacheck360.databinding.ActivityLogsBinding

class LogsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLogsBinding
    private lateinit var database: DatabaseReference
    private lateinit var adapter: LogsAdapter
    private var logsList = mutableListOf<LogItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLogsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // RecyclerView setup
        adapter = LogsAdapter(logsList)
        binding.rvLogs.layoutManager = LinearLayoutManager(this)
        binding.rvLogs.adapter = adapter

        // Firebase reference
        database = FirebaseDatabase.getInstance().reference.child("logs")

        fetchLogs()

        // Search filter
        binding.searchBar.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                filterLogs(s.toString())
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun fetchLogs() {
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                logsList.clear()
                for (logSnapshot in snapshot.children) {
                    val domain = logSnapshot.child("domain").getValue(String::class.java) ?: ""
                    val status = logSnapshot.child("status").getValue(String::class.java) ?: ""
                    val timestamp = logSnapshot.child("timestamp").getValue(Long::class.java) ?: 0L

                    val logItem = LogItem(domain, status, timestamp)
                    logsList.add(logItem)
                }
                adapter.updateList(logsList)
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle error
            }
        })
    }

    private fun filterLogs(query: String) {
        val filteredList = logsList.filter {
            it.domain.contains(query, ignoreCase = true) ||
                    it.status.contains(query, ignoreCase = true)
        }
        adapter.updateList(filteredList)
    }
}
