package com.pplm.projectinventarisuas.utils.viewmodel

import androidx.lifecycle.ViewModel
import com.pplm.projectinventarisuas.data.model.Item
import com.pplm.projectinventarisuas.data.repository.ItemRepository

class AddItemViewModel(private val itemRepository: ItemRepository) : ViewModel() {

    fun addItem(item: Item, callback: (Boolean) -> Unit) {
        itemRepository.addItem(item, callback)
    }

    fun itemExists(itemId: String, callback: (Boolean) -> Unit) {
        itemRepository.itemExists(itemId, callback)
    }
}