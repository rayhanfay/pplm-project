package com.pplm.projectinventarisuas.utils.viewmodel

import android.util.Log
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

    private val _phoneNumberChangeResult = MutableLiveData<Boolean>()
    val phoneNumberChangeResult: LiveData<Boolean> = _phoneNumberChangeResult

    private val _currentPassword = MutableLiveData<String?>()
    val currentPassword: LiveData<String?> get() = _currentPassword

    fun login(username: String, password: String) {
        Log.d("UserViewModel", "Login dipanggil dengan Username: $username")
        repository.login(username, password) { result ->
            Log.d("UserViewModel", "Hasil login dari Repository: $result")
            _user.postValue(result)
        }
    }

    fun changePassword(userId: String, userRole: String, newPassword: String) {
        Log.d("UserViewModel", "changePassword dipanggil untuk User ID: $userId, Peran: $userRole")
        repository.changePassword(userId, userRole, newPassword) { success ->
            Log.d("UserViewModel", "Hasil changePassword dari Repository: $success")
            _passwordChangeResult.postValue(success)
        }
    }

    fun updatePhoneNumber(userId: String, userRole: String, phoneNumber: String) {
        repository.updatePhoneNumber(userId, userRole, phoneNumber) { success ->
            _phoneNumberChangeResult.postValue(success)
        }
    }

    fun fetchCurrentPassword(userId: String, userRole: String) {
        Log.d("UserViewModel", "fetchCurrentPassword dipanggil untuk User ID: $userId, Peran: $userRole")
        repository.fetchCurrentPassword(userId, userRole) { password ->
            Log.d("UserViewModel", "Hasil fetchCurrentPassword dari Repository: $password")
            _currentPassword.postValue(password)
        }
    }

    fun isPasswordSameAsCurrent(userId: String, userRole: String, newPassword: String, callback: (Boolean) -> Unit) {
        Log.d("UserViewModel", "isPasswordSameAsCurrent dipanggil untuk User ID: $userId, Peran: $userRole")
        repository.isPasswordSameAsCurrent(userId, userRole, newPassword) { isSame ->
            Log.d("UserViewModel", "Hasil isPasswordSameAsCurrent dari Repository: $isSame")
            callback(isSame)
        }
    }
}
