package com.pplm.projectinventarisuas.data.dao

import com.pplm.projectinventarisuas.data.model.User

interface UserDao {
    fun login(username: String, password: String, callback: (User ?) -> Unit)
}
