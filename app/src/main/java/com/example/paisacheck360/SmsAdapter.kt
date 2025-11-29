package com.example.paisacheck360

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ScamAdapter(
    private val originalList: MutableList<ScamData>,
    private val onBlock: (ScamData) -> Unit,
    private val onReport: (ScamData) -> Unit
) : RecyclerView.Adapter<ScamAdapter.ScamViewHolder>() {

    private var filteredList = mutableListOf<ScamData>()

    init {
        filteredList.addAll(originalList)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScamViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_scam_card, parent, false)
        return ScamViewHolder(view)
    }

    override fun getItemCount(): Int = filteredList.size

    override fun onBindViewHolder(holder: ScamViewHolder, position: Int) {
        val scam = filteredList[position]
        holder.sender.text = "From: ${scam.sender}"
        holder.message.text = scam.body
        holder.riskBadge.text = when (scam.risk) {
            "Critical" -> "🔴 CRITICAL"
            "High" -> "🟠 HIGH"
            "Medium" -> "🟡 MEDIUM"
            else -> "🟢 SAFE"
        }

        holder.reportBtn.setOnClickListener { onReport(scam) }
        holder.blockBtn.setOnClickListener { onBlock(scam) }
    }

    fun filterByRisk(level: String) {
        filteredList.clear()
        if (level == "All") {
            filteredList.addAll(originalList)
        } else {
            filteredList.addAll(originalList.filter { it.risk.equals(level, true) })
        }
        notifyDataSetChanged()
    }

    class ScamViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val sender: TextView = view.findViewById(R.id.senderText)
        val message: TextView = view.findViewById(R.id.messageText)
        val riskBadge: TextView = view.findViewById(R.id.riskBadge)
        val reportBtn: Button = view.findViewById(R.id.reportBtn)
        val blockBtn: Button = view.findViewById(R.id.blockBtn)
    }
}
