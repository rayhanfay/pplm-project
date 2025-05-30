package com.pplm.projectinventarisuas.data.dao

import com.pplm.projectinventarisuas.data.model.Item

interface ItemDao {
    fun getItems(callback: (List<Item>) -> Unit)
    fun deleteItem(item: Item, callback: (Boolean) -> Unit)
    fun addItem(item: Item, callback: (Boolean) -> Unit)
    fun itemExists(itemId: String, callback: (Boolean) -> Unit)
    fun updateItem(item: Item, callback: (Boolean) -> Unit)
    fun updateItemStatus(itemId: String, newStatus: String, callback: (Boolean) -> Unit)
    fun getItemById(itemId: String, callback: (Item?) -> Unit)
}