package com.pplm.projectinventarisuas.data.repository

import com.pplm.projectinventarisuas.data.dao.BorrowingDao
import com.pplm.projectinventarisuas.data.database.DatabaseProvider
import com.pplm.projectinventarisuas.data.model.Borrowing
import com.pplm.projectinventarisuas.data.model.Item

class BorrowingRepository : BorrowingDao {

    private val database = DatabaseProvider.getDatabaseReference()

    override fun getBorrowingData(callback: (List<Borrowing>) -> Unit) {
        database.child("borrowing").get().addOnSuccessListener { snapshot ->
            val borrowingList = snapshot.children.mapNotNull { it.getValue(Borrowing::class.java) }
            fetchUserNames(borrowingList, callback)
        }.addOnFailureListener {
            callback(emptyList())
        }
    }

    private fun fetchUserNames(
        borrowingList: List<Borrowing>,
        callback: (List<Borrowing>) -> Unit
    ) {
        val updatedBorrowingList = borrowingList.toMutableList()
        var count = 0

        for (borrowing in borrowingList) {
            database.child("admin").child(borrowing.admin_id).get()
                .addOnSuccessListener { adminSnapshot ->
                    val adminName = adminSnapshot.child("admin_name").value.toString()
                    val updatedBorrowing = borrowing.copy(admin_name = adminName)

                    database.child("student").child(borrowing.student_id).get()
                        .addOnSuccessListener { studentSnapshot ->
                            val studentName = studentSnapshot.child("student_name").value.toString()
                            val finalBorrowing = updatedBorrowing.copy(student_name = studentName)

                            updatedBorrowingList[count] = finalBorrowing
                            count++

                            if (count == borrowingList.size) {
                                callback(updatedBorrowingList)
                            }
                        }.addOnFailureListener {
                            count++
                            if (count == borrowingList.size) {
                                callback(updatedBorrowingList)
                            }
                        }
                }.addOnFailureListener {
                    count++
                    if (count == borrowingList.size) {
                        callback(updatedBorrowingList)
                    }
                }
        }
    }

    fun getBorrowingByItemId(itemId: String, callback: (List<Borrowing>) -> Unit) {
        database.child("borrowing").get().addOnSuccessListener { snapshot ->
            val borrowingList = snapshot.children.mapNotNull { it.getValue(Borrowing::class.java) }
            val filteredList = borrowingList.filter { it.item_id == itemId }
            callback(filteredList)
        }.addOnFailureListener {
            callback(emptyList())
        }
    }

    fun getBorrowingsWithStatus(
        itemId: String,
        status: String,
        callback: (List<Borrowing>) -> Unit
    ) {
        database.child("borrowing").get().addOnSuccessListener { snapshot ->
            val borrowingList = snapshot.children.mapNotNull { it.getValue(Borrowing::class.java) }
            val filteredList = borrowingList.filter { it.item_id == itemId && it.status == status }
            callback(filteredList)
        }.addOnFailureListener {
            callback(emptyList())
        }
    }

    fun updateBorrowingStatus(
        borrowingId: String,
        status: String,
        returnTime: String,
        callback: (Boolean) -> Unit
    ) {
        val borrowingRef = database.child("borrowing").child(borrowingId)
        borrowingRef.child("status").setValue(status)
        borrowingRef.child("return_time").setValue(returnTime)
            .addOnSuccessListener { callback(true) }
            .addOnFailureListener { callback(false) }
    }

    fun fetchItemById(itemId: String, callback: (Item?) -> Unit) {
        database.child("item").child(itemId).get()
            .addOnSuccessListener {
                callback(it.getValue(Item::class.java))
            }
            .addOnFailureListener {
                callback(null)
            }
    }

    fun fetchAdminMap(callback: (Map<String, String>) -> Unit) {
        database.child("admin").get().addOnSuccessListener { snapshot ->
            val map = mutableMapOf<String, String>()
            snapshot.children.forEach {
                val name = it.child("admin_name").getValue(String::class.java)
                val id = it.child("admin_id").getValue(String::class.java)
                if (name != null && id != null) map[name] = id
            }
            callback(map)
        }.addOnFailureListener {
            callback(emptyMap())
        }
    }

    fun saveBorrowingAndUpdateItem(
        borrowingData: Map<String, String>,
        itemId: String,
        callback: (Boolean, String?) -> Unit
    ) {
        val borrowingId = borrowingData["borrowing_id"] ?: return callback(false, "Invalid ID")
        database.child("borrowing").child(borrowingId).setValue(borrowingData)
            .addOnSuccessListener {
                database.child("item").child(itemId).child("item_status").setValue("In Use")
                    .addOnSuccessListener {
                        callback(true, null)
                    }
                    .addOnFailureListener { callback(false, it.message) }
            }
            .addOnFailureListener {
                callback(false, it.message)
            }
    }

    override fun deleteBorrowing(borrowing: Borrowing, callback: (Boolean) -> Unit) {
        database.child("borrowing").child(borrowing.borrowing_id).removeValue()
            .addOnSuccessListener { callback(true) }
            .addOnFailureListener { callback(false) }
    }
}
