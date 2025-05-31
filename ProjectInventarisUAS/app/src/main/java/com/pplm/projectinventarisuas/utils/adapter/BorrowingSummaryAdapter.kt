package com.pplm.projectinventarisuas.utils.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.pplm.projectinventarisuas.R
import com.pplm.projectinventarisuas.data.model.BorrowingSummary

class BorrowingSummaryAdapter(private val summaryList: List<BorrowingSummary>) :
    RecyclerView.Adapter<BorrowingSummaryAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvStatus: TextView = view.findViewById(R.id.tvBorrowingStatus)
        val tvCount: TextView = view.findViewById(R.id.tvBorrowingCount)
        val ivStatusIcon: ImageView =
            view.findViewById(R.id.ivBorrowingStatusIcon)
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

        when (borrowing.status.lowercase()) {
            "hilang" -> holder.ivStatusIcon.setImageResource(R.drawable.ic_lost)
            "dikembalikan" -> holder.ivStatusIcon.setImageResource(R.drawable.ic_returned)
            "dipinjam" -> holder.ivStatusIcon.setImageResource(R.drawable.ic_borrowed)
            "terlambat" -> holder.ivStatusIcon.setImageResource(R.drawable.ic_late)
            else -> holder.ivStatusIcon.setImageResource(R.drawable.ic_default_image)
        }
    }
}