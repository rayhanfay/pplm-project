package com.pplm.projectinventarisuas.data.repository

import com.pplm.projectinventarisuas.data.database.DatabaseProvider
import com.pplm.projectinventarisuas.data.model.Item

class ItemRepository {

    private val database = DatabaseProvider.getDatabaseReference()

    fun getItems(callback: (List<Item>) -> Unit) {
        database.child("item").get().addOnSuccessListener { snapshot ->
            val itemList = snapshot.children.mapNotNull { it.getValue(Item::class.java) }
            callback(itemList)
        }.addOnFailureListener {
            callback(emptyList())
        }
    }

    fun deleteItem(item: Item, callback: (Boolean) -> Unit) {
        database.child("item").child(item.item_id).removeValue()
            .addOnSuccessListener { callback(true) }
            .addOnFailureListener { callback(false) }
    }

    fun addItem(item: Item, callback: (Boolean) -> Unit) {
        database.child("item").child(item.item_id).setValue(item)
            .addOnSuccessListener { callback(true) }
            .addOnFailureListener { callback(false) }
    }

    fun itemExists(itemId: String, callback: (Boolean) -> Unit) {
        database.child("item").child(itemId).get().addOnSuccessListener { snapshot ->
            callback(snapshot.exists())
        }.addOnFailureListener {
            callback(false)
        }
    }
}