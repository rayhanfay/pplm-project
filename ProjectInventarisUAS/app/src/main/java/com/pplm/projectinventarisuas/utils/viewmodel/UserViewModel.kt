package com.pplm.projectinventarisuas.utils.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.pplm.projectinventarisuas.data.model.User
import com.pplm.projectinventarisuas.data.repository.UserRepository

class UserViewModel(private val repository: UserRepository) : ViewModel() {

    private val _user = MutableLiveData<User?>()
    val user: LiveData<User?> get() = _user

    fun login(username: String, password: String) {
        repository.login(username, password) { result ->
            _user.postValue(result)
        }
    }
}