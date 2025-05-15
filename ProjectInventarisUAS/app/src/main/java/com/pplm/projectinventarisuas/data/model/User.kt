package com.pplm.projectinventarisuas.data.model

data class User(
    val role: String,
    val name: String,
    val id: String? = null,
    val isPasswordChanged: Boolean = false
)
