package com.pplm.projectinventarisuas.utils.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.pplm.projectinventarisuas.data.model.Borrowing
import com.pplm.projectinventarisuas.data.repository.BorrowingRepository

class BorrowingViewModel(private val repository: BorrowingRepository) : ViewModel() {
    private val _borrowingList = MutableLiveData<List<Borrowing>>()
    val borrowingList: LiveData<List<Borrowing>> get() = _borrowingList

    fun loadBorrowingData() {
        repository.getBorrowingData { list ->
            _borrowingList.value = list
        }
    }

    fun deleteBorrowing(borrowing: Borrowing) {
        repository.deleteBorrowing(borrowing) { success ->
            if (success) {
                loadBorrowingData()
            }
        }
    }
}