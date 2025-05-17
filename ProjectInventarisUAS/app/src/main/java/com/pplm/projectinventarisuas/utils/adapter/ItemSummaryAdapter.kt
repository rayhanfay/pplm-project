package com.pplm.projectinventarisuas.utils.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.pplm.projectinventarisuas.R
import com.pplm.projectinventarisuas.data.model.ItemSummary

class ItemSummaryAdapter(private val summaryList: List<ItemSummary>) :
    RecyclerView.Adapter<ItemSummaryAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvType: TextView = view.findViewById(R.id.tvItemType)
        val tvCount: TextView = view.findViewById(R.id.tvItemCount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_summary_card, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = summaryList.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = summaryList[position]
        holder.tvType.text = item.type
        holder.tvCount.text = "${item.count} item"
    }
}
