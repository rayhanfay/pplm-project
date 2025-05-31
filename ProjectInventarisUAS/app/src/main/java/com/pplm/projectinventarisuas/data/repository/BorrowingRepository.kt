package com.pplm.projectinventarisuas.data.repository

import android.util.Log
import com.pplm.projectinventarisuas.data.dao.BorrowingDao
import com.pplm.projectinventarisuas.data.database.DatabaseProvider
import com.pplm.projectinventarisuas.data.model.Borrowing
import com.pplm.projectinventarisuas.data.model.Item
import java.util.concurrent.atomic.AtomicInteger

class BorrowingRepository : BorrowingDao {

    private val database = DatabaseProvider.getDatabaseReference()

    override fun getBorrowingData(callback: (List<Borrowing>) -> Unit) {
        database.child("borrowing").get().addOnSuccessListener { snapshot ->
            val borrowingList = snapshot.children.mapNotNull { it.getValue(Borrowing::class.java) }
            fetchRelatedData(borrowingList, callback)
        }.addOnFailureListener {
            Log.e("BorrowingRepo", "Gagal mendapatkan data peminjaman dari Firebase", it)
            callback(emptyList())
        }
    }

    private fun fetchRelatedData(
        borrowingList: List<Borrowing>,
        callback: (List<Borrowing>) -> Unit
    ) {
        if (borrowingList.isEmpty()) {
            Log.d("BorrowingRepo", "Daftar peminjaman kosong, mengembalikan daftar kosong.")
            callback(emptyList())
            return
        }

        val totalExpectedOperations = borrowingList.size * 3
        val completedOperations = AtomicInteger(0)

        val interimBorrowings = mutableMapOf<String, Borrowing>()
        borrowingList.forEach { borrowing ->
            interimBorrowings[borrowing.borrowing_id] = borrowing.copy()
        }

        val checkCompletion = {
            val currentCompleted = completedOperations.incrementAndGet()
            Log.d(
                "BorrowingRepo",
                "Operasi selesai: $currentCompleted / $totalExpectedOperations"
            )
            if (currentCompleted == totalExpectedOperations) {
                val finalBorrowingList = borrowingList.map { original ->
                    interimBorrowings[original.borrowing_id]
                        ?: original
                }
                Log.d(
                    "BorrowingRepo",
                    "Semua operasi selesai. Memanggil callback dengan ${finalBorrowingList.size} item."
                )
                callback(finalBorrowingList)
            }
        }

        for (originalBorrowing in borrowingList) {
            val borrowingId = originalBorrowing.borrowing_id

            database.child("admin").child(originalBorrowing.admin_id).get()
                .addOnSuccessListener { adminSnapshot ->
                    val adminName = adminSnapshot.child("admin_name").value.toString()
                    val adminPhoneNumber = adminSnapshot.child("phone_number").value.toString()
                    synchronized(this) {
                        interimBorrowings[borrowingId] =
                            interimBorrowings[borrowingId]!!.copy(admin_name = adminName, admin_phone_number = adminPhoneNumber)
                    }
                    Log.d("BorrowingRepo", "Nama admin diambil untuk ID peminjaman: ${borrowingId}")
                    checkCompletion()
                }.addOnFailureListener { e ->
                    Log.e(
                        "BorrowingRepo",
                        "Gagal mengambil nama admin untuk ID peminjaman: ${borrowingId}",
                        e
                    )
                    checkCompletion()
                }

            database.child("student").child(originalBorrowing.student_id).get()
                .addOnSuccessListener { studentSnapshot ->
                    val studentName = studentSnapshot.child("student_name").value.toString()
                    val studentPhoneNumber = studentSnapshot.child("phone_number").value.toString()
                    synchronized(this) {
                        interimBorrowings[borrowingId] =
                            interimBorrowings[borrowingId]!!.copy(student_name = studentName, student_phone_number = studentPhoneNumber) // Tambahkan student_phone_number
                    }
                    Log.d("BorrowingRepo", "Nama siswa diambil untuk ID peminjaman: ${borrowingId}")
                    checkCompletion()
                }.addOnFailureListener { e ->
                    Log.e(
                        "BorrowingRepo",
                        "Gagal mengambil nama siswa untuk ID peminjaman: ${borrowingId}",
                        e
                    )
                    checkCompletion()
                }

            database.child("item").child(originalBorrowing.item_id).get()
                .addOnSuccessListener { itemSnapshot ->
                    val itemName = itemSnapshot.child("item_name").value.toString()
                    val itemType = itemSnapshot.child("item_type").value.toString()
                    synchronized(this) {
                        interimBorrowings[borrowingId] =
                            interimBorrowings[borrowingId]!!.copy(item_name = itemName, item_type = itemType)
                    }
                    Log.d(
                        "BorrowingRepo",
                        "Nama dan tipe item diambil untuk ID peminjaman: ${borrowingId}, item: $itemName, tipe: $itemType"
                    )
                    checkCompletion()
                }.addOnFailureListener { e ->
                    Log.e(
                        "BorrowingRepo",
                        "Gagal mengambil nama dan tipe item untuk ID peminjaman: ${borrowingId}",
                        e
                    )
                    checkCompletion()
                }
        }
    }

    fun getBorrowingByItemId(itemId: String, callback: (List<Borrowing>) -> Unit) {
        database.child("borrowing").get().addOnSuccessListener { snapshot ->
            val borrowingList = snapshot.children.mapNotNull { it.getValue(Borrowing::class.java) }
            val filteredList = borrowingList.filter { it.item_id == itemId }
            fetchRelatedData(filteredList, callback)
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
            fetchRelatedData(filteredList, callback)
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
        val borrowingId = borrowingData["borrowing_id"] ?: return callback(false, "ID tidak valid")
        database.child("borrowing").child(borrowingId).setValue(borrowingData)
            .addOnSuccessListener {
                database.child("item").child(itemId).child("item_status").setValue("Sedang Digunakan")
                    .addOnSuccessListener {
                        callback(true, null)
                    }
                    .addOnFailureListener { callback(false, it.message) }
            }
            .addOnFailureListener {
                callback(false, it.message)
            }
    }

    fun updateBorrowing(borrowing: Borrowing, callback: (Boolean) -> Unit) {
        database.child("borrowing").child(borrowing.borrowing_id).setValue(borrowing)
            .addOnSuccessListener { callback(true) }
            .addOnFailureListener { callback(false) }
    }

    override fun deleteBorrowing(borrowing: Borrowing, callback: (Boolean) -> Unit) {
        database.child("borrowing").child(borrowing.borrowing_id).removeValue()
            .addOnSuccessListener { callback(true) }
            .addOnFailureListener { callback(false) }
    }
}