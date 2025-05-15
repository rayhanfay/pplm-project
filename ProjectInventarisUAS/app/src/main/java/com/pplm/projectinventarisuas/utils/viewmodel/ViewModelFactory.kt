package com.pplm.projectinventarisuas.utils.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.pplm.projectinventarisuas.data.repository.BorrowingRepository
import com.pplm.projectinventarisuas.data.repository.ItemRepository
import com.pplm.projectinventarisuas.data.repository.UserRepository

@Suppress("UNCHECKED_CAST")
class ViewModelFactory(
    private val itemRepository: ItemRepository,
    private val borrowingRepository: BorrowingRepository,
    private val userRepository: UserRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ItemViewModel::class.java)) {
            return ItemViewModel(itemRepository) as T
        }
        if (modelClass.isAssignableFrom(BorrowingViewModel::class.java)) {
            return BorrowingViewModel(borrowingRepository) as T
        }
        if (modelClass.isAssignableFrom(UserViewModel::class.java)) {
            return UserViewModel(userRepository) as T
        }
        if (modelClass.isAssignableFrom(AddItemViewModel::class.java)) {
            return AddItemViewModel(itemRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
