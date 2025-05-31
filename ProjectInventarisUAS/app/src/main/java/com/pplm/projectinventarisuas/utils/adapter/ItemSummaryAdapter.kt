package com.pplm.projectinventarisuas.utils.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.pplm.projectinventarisuas.R
import com.pplm.projectinventarisuas.data.model.ItemSummary

class ItemSummaryAdapter(private var summaryList: List<ItemSummary>) :
    RecyclerView.Adapter<ItemSummaryAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvType: TextView = view.findViewById(R.id.tvItemType)
        val tvCount: TextView = view.findViewById(R.id.tvItemCount)
        val ivItemImage: ImageView = view.findViewById(R.id.ivItemImage)
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

        when (item.type.lowercase()) {
            "remote" -> holder.ivItemImage.setImageResource(R.drawable.ic_remote)
            "kabel" -> holder.ivItemImage.setImageResource(R.drawable.ic_cable)
            "ekstensi" -> holder.ivItemImage.setImageResource(R.drawable.ic_extension)
            "proyektor" -> holder.ivItemImage.setImageResource(R.drawable.ic_projector)
            else -> holder.ivItemImage.setImageResource(R.drawable.ic_default_image)
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateItems(newItems: List<ItemSummary>) {
        summaryList = newItems
        notifyDataSetChanged()
    }
}