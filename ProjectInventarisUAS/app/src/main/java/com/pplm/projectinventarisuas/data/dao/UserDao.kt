package com.pplm.projectinventarisuas.data.dao

import com.pplm.projectinventarisuas.data.model.User

interface UserDao {
    fun login(username: String, password: String, callback: (User?) -> Unit)
    fun changePassword(
        userId: String,
        userRole: String,
        newPassword: String,
        callback: (Boolean) -> Unit
    )

    fun fetchCurrentPassword(userId: String, userRole: String, callback: (String?) -> Unit)
    fun isPasswordSameAsCurrent(
        userId: String,
        userRole: String,
        newPassword: String,
        callback: (Boolean) -> Unit
    )
}