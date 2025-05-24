package com.pplm.projectinventarisuas.utils.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.pplm.projectinventarisuas.data.model.Borrowing
import com.pplm.projectinventarisuas.data.model.Item
import com.pplm.projectinventarisuas.data.repository.ItemRepository
import com.pplm.projectinventarisuas.databinding.ItemBorrowingLayoutBinding

class BorrowingAdapter(
    private val itemList: List<Borrowing>,
    private val itemRepository: ItemRepository,
    private val onItemClick: (Borrowing) -> Unit
) : RecyclerView.Adapter<BorrowingAdapter.ItemViewHolder>() {

    inner class ItemViewHolder(private val binding: ItemBorrowingLayoutBinding) :
        RecyclerView.ViewHolder(binding.root) {
        @SuppressLint("SetTextI19n")
        fun bind(item: Borrowing, position: Int) {
            binding.tvNumber.text = (position + 1).toString()
            binding.tvBorrowingCode.text = item.item_id
            binding.tvBorrowingStatus.text = item.status

            binding.tvItemName.text = "Memuat Nama Item..."

            itemRepository.getItems { allItems ->
                val foundItem = allItems.find { it.item_id == item.item_id }
                if (foundItem != null) {
                    binding.tvItemName.text = foundItem.item_name
                } else {
                    binding.tvItemName.text = "Nama Item Tidak Ditemukan"
                }
            }

            binding.root.setOnClickListener {
                onItemClick(item)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val binding =
            ItemBorrowingLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ItemViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        holder.bind(itemList[position], position)
    }

    override fun getItemCount() = itemList.size
}