package com.pplm.projectinventarisuas.data.dao

import com.pplm.projectinventarisuas.data.model.Borrowing

interface BorrowingDao {
    fun getBorrowingData(callback: (List<Borrowing>) -> Unit)
    fun deleteBorrowing(borrowing: Borrowing, callback: (Boolean) -> Unit)
}