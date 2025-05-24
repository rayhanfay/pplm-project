package com.pplm.projectinventarisuas.utils.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.pplm.projectinventarisuas.R
import com.pplm.projectinventarisuas.data.model.BorrowingSummary

class BorrowingSummaryAdapter(private val summaryList: List<BorrowingSummary>) :
    RecyclerView.Adapter<BorrowingSummaryAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvStatus: TextView = view.findViewById(R.id.tvBorrowingStatus)
        val tvCount: TextView = view.findViewById(R.id.tvBorrowingCount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.borrowing_summary_card, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = summaryList.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val borrowing = summaryList[position]
        holder.tvStatus.text = borrowing.status
        holder.tvCount.text = "${borrowing.count} borrowing"
    }
}
