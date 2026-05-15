package com.example.scrolltracker.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.scrolltracker.R
import com.example.scrolltracker.data.DailyRecord
import com.example.scrolltracker.utils.NotificationHelper

class HistoryAdapter(private val historyList: List<DailyRecord>) :
    RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvDate: TextView = view.findViewById(R.id.tvDate)
        val tvCount: TextView = view.findViewById(R.id.tvCount)
        val tvWatchTime: TextView = view.findViewById(R.id.tvWatchTime)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_history, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val record = historyList[position]
        holder.tvDate.text = record.date
        holder.tvCount.text = "${record.count} scrolls"
        holder.tvWatchTime.text = if (record.watchTimeMs > 0L)
            "Watch time: ${NotificationHelper.formatWatchTime(record.watchTimeMs)}"
        else
            "Watch time: —"
    }

    override fun getItemCount() = historyList.size
}
