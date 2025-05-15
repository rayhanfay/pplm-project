package com.pplm.projectinventarisuas.utils.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.pplm.projectinventarisuas.data.model.Item
import com.pplm.projectinventarisuas.databinding.ItemLayoutBinding

class ItemAdapter(
    private var itemList: List<Item>,
    private val onItemClick: (Item) -> Unit
) : RecyclerView.Adapter<ItemAdapter.ItemViewHolder>() {

    inner class ItemViewHolder(private val binding: ItemLayoutBinding) :
        RecyclerView.ViewHolder(binding.root) {

        @SuppressLint("SetTextI18n")
        fun bind(item: Item, position: Int) {
            binding.tvNumber.text = (position + 1).toString()
            binding.tvItemCode.text = item.item_id
            binding.tvItemName.text = item.item_name
            binding.tvItemStatus.text = item.item_status

            binding.root.setOnClickListener {
                onItemClick(item)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val binding = ItemLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ItemViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        holder.bind(itemList[position], position)
    }

    override fun getItemCount() = itemList.size

    @SuppressLint("NotifyDataSetChanged")
    fun updateItems(newItems: List<Item>) {
        itemList = newItems
        notifyDataSetChanged()
    }
}
