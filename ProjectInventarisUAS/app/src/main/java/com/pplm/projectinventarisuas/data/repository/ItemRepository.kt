package com.pplm.projectinventarisuas.data.repository

import com.pplm.projectinventarisuas.data.database.DatabaseProvider
import com.pplm.projectinventarisuas.data.model.Item
import com.pplm.projectinventarisuas.data.dao.ItemDao
import com.google.firebase.database.DatabaseReference

class ItemRepository : ItemDao {

    private val database: DatabaseReference = DatabaseProvider.getDatabaseReference()

    private fun Item.toMap(): Map<String, Any> = mapOf(
        "item_id" to item_id,
        "item_name" to item_name,
        "item_type" to item_type,
        "item_status" to item_status,
        "item_description" to item_description
    )

    override fun getItems(callback: (List<Item>) -> Unit) {
        database.child("item").get().addOnSuccessListener { snapshot ->
            val itemList = snapshot.children.mapNotNull { it.getValue(Item::class.java) }
            callback(itemList)
        }.addOnFailureListener {
            callback(emptyList())
        }
    }

    override fun deleteItem(item: Item, callback: (Boolean) -> Unit) {
        database.child("item").child(item.item_id).removeValue()
            .addOnSuccessListener { callback(true) }
            .addOnFailureListener { callback(false) }
    }

    override fun addItem(item: Item, callback: (Boolean) -> Unit) {
        database.child("item").child(item.item_id).setValue(item.toMap())
            .addOnSuccessListener { callback(true) }
            .addOnFailureListener { callback(false) }
    }

    override fun itemExists(itemId: String, callback: (Boolean) -> Unit) {
        database.child("item").child(itemId).get().addOnSuccessListener { snapshot ->
            callback(snapshot.exists())
        }.addOnFailureListener {
            callback(false)
        }
    }

    override fun updateItem(item: Item, callback: (Boolean) -> Unit) {
        database.child("item").child(item.item_id).setValue(item.toMap())
            .addOnSuccessListener { callback(true) }
            .addOnFailureListener { callback(false) }
    }

    override fun updateItemStatus(itemId: String, newStatus: String, callback: (Boolean) -> Unit) {
        database.child("item").child(itemId).child("item_status").setValue(newStatus)
            .addOnSuccessListener { callback(true) }
            .addOnFailureListener { callback(false) }
    }

    override fun getItemById(itemId: String, callback: (Item?) -> Unit) {
        database.child("item").child(itemId).get().addOnSuccessListener { snapshot ->
            val item = snapshot.getValue(Item::class.java)
            callback(item)
        }.addOnFailureListener {
            callback(null)
        }
    }
}