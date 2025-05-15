package com.pplm.projectinventarisuas.utils.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.pplm.projectinventarisuas.data.model.Item
import com.pplm.projectinventarisuas.data.repository.ItemRepository

class ItemViewModel(private val repository: ItemRepository) : ViewModel() {
    private val _items = MutableLiveData<List<Item>>()
    val items: LiveData<List<Item>> get() = _items

    fun loadItems() {
        repository.getItems { itemList ->
            _items.value = itemList
        }
    }

    fun deleteItem(item: Item) {
        repository.deleteItem(item) { success ->
            if (success) {
                loadItems()
            }
        }
    }
}