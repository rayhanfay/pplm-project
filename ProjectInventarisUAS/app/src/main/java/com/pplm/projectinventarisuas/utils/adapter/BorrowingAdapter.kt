package com.pplm.projectinventarisuas.utils.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.pplm.projectinventarisuas.R
import com.pplm.projectinventarisuas.data.model.Borrowing
import com.pplm.projectinventarisuas.databinding.ItemBorrowingLayoutBinding

class BorrowingAdapter(
    private val itemList: List<Borrowing>,
    private val onItemClick: (Borrowing) -> Unit
) : RecyclerView.Adapter<BorrowingAdapter.ItemViewHolder>() {

    inner class ItemViewHolder(private val binding: ItemBorrowingLayoutBinding) :
        RecyclerView.ViewHolder(binding.root) {
        @SuppressLint("SetTextI18n")
        fun bind(item: Borrowing, position: Int) {
            binding.tvNumber.text = (position + 1).toString()
            binding.tvBorrowingCode.text = item.borrowing_id
            binding.tvBorrowingStatus.text = item.status
            binding.tvItemName.text = item.item_name

            val drawableResId = getDrawableResId(item.status, item.item_type)
            binding.ivBorrowingItemIcon.setImageResource(drawableResId)

            binding.root.setOnClickListener {
                onItemClick(item)
            }
        }

        private fun getDrawableResId(status: String, itemType: String): Int {
            val lowerCaseStatus = status.lowercase()
            val lowerCaseItemType = itemType.lowercase()

            return when (lowerCaseStatus) {
                "borrowed" -> when (lowerCaseItemType) {
                    "remote" -> R.drawable.ic_remote_borrowed
                    "kabel" -> R.drawable.ic_cable_borrowed
                    "extension" -> R.drawable.ic_extension_borrowed
                    "proyektor" -> R.drawable.ic_projector_borrowed
                    else -> R.drawable.ic_borrowed
                }
                "returned" -> when (lowerCaseItemType) {
                    "remote" -> R.drawable.ic_remote_returned
                    "kabel" -> R.drawable.ic_cable_returned
                    "extension" -> R.drawable.ic_extension_returned
                    "proyektor" -> R.drawable.ic_projector_returned
                    else -> R.drawable.ic_returned
                }
                "lost" -> when (lowerCaseItemType) {
                    "remote" -> R.drawable.ic_remote_lost
                    "kabel" -> R.drawable.ic_cable_lost
                    "extension" -> R.drawable.ic_extension_lost
                    "proyektor" -> R.drawable.ic_projector_lost
                    else -> R.drawable.ic_lost
                }
                "late" -> when (lowerCaseItemType) {
                    "remote" -> R.drawable.ic_remote_late
                    "kabel" -> R.drawable.ic_cable_late
                    "extension" -> R.drawable.ic_extension_late
                    "proyektor" -> R.drawable.ic_projector_late
                    else -> R.drawable.ic_late
                }
                else -> R.drawable.ic_default_image
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