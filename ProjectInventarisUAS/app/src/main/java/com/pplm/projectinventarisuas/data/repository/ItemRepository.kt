package com.pplm.projectinventarisuas.data.repository

import com.pplm.projectinventarisuas.data.database.DatabaseProvider
import com.pplm.projectinventarisuas.data.model.Item

class ItemRepository {

    private val database = DatabaseProvider.getDatabaseReference()

    fun Item.toMap(): Map<String, Any> = mapOf(
        "item_id" to item_id,
        "item_name" to item_name,
        "item_type" to item_type,
        "item_status" to item_status,
        "item_description" to item_description
    )

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
        database.child("item").child(item.item_id).setValue(item.toMap())
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

    fun updateItem(item: Item, callback: (Boolean) -> Unit) {
        database.child("item").child(item.item_id).setValue(item.toMap())
            .addOnSuccessListener { callback(true) }
            .addOnFailureListener { callback(false) }
    }

    fun updateItemStatus(itemId: String, newStatus: String, callback: (Boolean) -> Unit) {
        database.child("item").child(itemId).child("item_status").setValue(newStatus)
            .addOnSuccessListener { callback(true) }
            .addOnFailureListener { callback(false) }
    }
}