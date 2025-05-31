package com.pplm.projectinventarisuas.utils.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.pplm.projectinventarisuas.data.model.Item
import com.pplm.projectinventarisuas.data.repository.ItemRepository

class ItemViewModel(private val repository: ItemRepository) : ViewModel() {

    private val allItems = mutableListOf<Item>()

    private val _items = MutableLiveData<List<Item>>()
    val items: LiveData<List<Item>> get() = _items

    private val _summaryItems = MutableLiveData<List<Item>>()
    val summaryItems: LiveData<List<Item>> get() = _summaryItems

    fun loadItemsAvailable() {
        repository.getItems { itemList ->
            allItems.clear()
            allItems.addAll(itemList)

            _items.value = itemList.filter { it.item_status == "Tersedia" }
            _summaryItems.value = itemList.filter { it.item_status == "Tersedia" }
        }
    }

    fun loadItems() {
        repository.getItems { itemList ->
            allItems.clear()
            allItems.addAll(itemList)

            _items.value = itemList
            _summaryItems.value = itemList
        }
    }

    fun searchItemsAvailable(query: String) {
        val filtered = allItems.filter { item ->
            item.item_status.equals("Tersedia", true) && (
                    item.item_name.contains(query, ignoreCase = true) ||
                            item.item_type.contains(query, ignoreCase = true) ||
                            item.item_id.contains(query, ignoreCase = true)
                    )
        }
        _items.value = filtered
    }

    fun searchItems(query: String) {
        val filtered = allItems.filter { item ->
            item.item_name.contains(query, ignoreCase = true) ||
                    item.item_type.contains(query, ignoreCase = true) ||
                    item.item_id.contains(query, ignoreCase = true)
        }
        _items.value = filtered
    }

    fun updateItem(item: Item) {
        repository.updateItem(item) { success ->
            if (success) loadItems()
        }
    }

    fun deleteItem(item: Item) {
        repository.deleteItem(item) { success ->
            if (success) loadItems()
        }
    }
}
