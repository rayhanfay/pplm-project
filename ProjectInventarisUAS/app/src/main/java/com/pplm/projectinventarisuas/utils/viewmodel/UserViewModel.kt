package com.pplm.projectinventarisuas.utils.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.pplm.projectinventarisuas.data.model.User
import com.pplm.projectinventarisuas.data.repository.UserRepository

class UserViewModel(private val repository: UserRepository) : ViewModel() {
    private val _user = MutableLiveData<User?>()
    val user: LiveData<User?> get() = _user

    private val _passwordChangeResult = MutableLiveData<Boolean>()
    val passwordChangeResult: LiveData<Boolean> get() = _passwordChangeResult

    private val _currentPassword = MutableLiveData<String?>()
    val currentPassword: LiveData<String?> get() = _currentPassword

    fun login(username: String, password: String) {
        repository.login(username, password) { result ->
            _user.postValue(result)
        }
    }

    fun changePassword(userId: String, userRole: String, newPassword: String) {
        repository.changePassword(userId, userRole, newPassword) { success ->
            _passwordChangeResult.postValue(success)
        }
    }

    fun fetchCurrentPassword(userId: String, userRole: String) {
        repository.fetchCurrentPassword(userId, userRole) { password ->
            _currentPassword.postValue(password)
        }
    }

    fun isPasswordSameAsCurrent(userId: String, userRole: String, newPassword: String, callback: (Boolean) -> Unit) {
        repository.isPasswordSameAsCurrent(userId, userRole, newPassword) { isSame ->
            callback(isSame)
        }
    }
}