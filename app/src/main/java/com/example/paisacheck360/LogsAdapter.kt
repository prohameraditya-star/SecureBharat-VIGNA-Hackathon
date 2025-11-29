package com.example.paisacheck360

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.paisacheck360.databinding.ItemLogBinding

class LogsAdapter(private var logs: List<LogItem>) :
    RecyclerView.Adapter<LogsAdapter.LogViewHolder>() {

    inner class LogViewHolder(private val binding: ItemLogBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: LogItem) {
            binding.tvDomain.text = item.domain
            binding.tvStatus.text = item.status
            binding.tvTimestamp.text = item.timestamp.toString()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogViewHolder {
        val binding = ItemLogBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return LogViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LogViewHolder, position: Int) {
        holder.bind(logs[position])
    }

    override fun getItemCount(): Int = logs.size

    fun updateList(newLogs: List<LogItem>) {
        logs = newLogs
        notifyDataSetChanged()
    }
}
