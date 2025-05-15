package com.pplm.projectinventarisuas.data.dao

import com.pplm.projectinventarisuas.data.model.Item

interface ItemDao {
    fun getItems(callback: (List<Item>) -> Unit)
    fun deleteItem(item: Item, callback: (Boolean) -> Unit)
}