package com.pplm.projectinventarisuas.utils.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.pplm.projectinventarisuas.data.model.Borrowing
import com.pplm.projectinventarisuas.data.model.Item
import com.pplm.projectinventarisuas.data.repository.BorrowingRepository

class BorrowingViewModel(private val repository: BorrowingRepository) : ViewModel() {
    private val _borrowingList = MutableLiveData<List<Borrowing>>()
    val borrowingList: LiveData<List<Borrowing>> get() = _borrowingList

    private val _adminMap = MutableLiveData<Map<String, String>>()
    val adminMap: LiveData<Map<String, String>> get() = _adminMap

    private val _itemName = MutableLiveData<String>()
    val itemName: LiveData<String> get() = _itemName

    private val _saveStatus = MutableLiveData<Pair<Boolean, String?>>()
    val saveStatus: LiveData<Pair<Boolean, String?>> get() = _saveStatus

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _lastBorrowingId = MutableLiveData<String>()
    val lastBorrowingId: LiveData<String> get() = _lastBorrowingId

    private val _summaryBorrowingStatus = MutableLiveData<List<Borrowing>>()
    val summaryBorrowingStatus: LiveData<List<Borrowing>> get() = _summaryBorrowingStatus

    fun loadBorrowingData() {
        _isLoading.value = true
        repository.getBorrowingData { list ->
            _borrowingList.value = list
            _summaryBorrowingStatus.value = list
            _isLoading.value = false
        }
    }

    fun deleteBorrowing(borrowing: Borrowing) {
        repository.deleteBorrowing(borrowing) { success ->
            if (success) {
                loadBorrowingData()
            }
        }
    }

    fun loadAdminMap() {
        repository.fetchAdminMap {
            _adminMap.value = it
        }
    }

    fun fetchItemName(itemId: String) {
        repository.fetchItemById(itemId) {
            _itemName.value = it?.item_name ?: "Unknown"
        }
    }

    fun saveBorrowing(data: Map<String, String>, itemId: String) {
        _lastBorrowingId.value = data["borrowing_id"]

        repository.saveBorrowingAndUpdateItem(data, itemId) { success, message ->
            _saveStatus.value = Pair(success, message)
        }
    }

    fun getAdminIdByName(adminName: String): String? {
        return adminMap.value?.get(adminName)
    }
}