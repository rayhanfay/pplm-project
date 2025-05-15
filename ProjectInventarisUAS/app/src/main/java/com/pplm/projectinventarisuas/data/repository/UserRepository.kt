package com.pplm.projectinventarisuas.data.repository

import com.pplm.projectinventarisuas.data.database.DatabaseProvider
import com.pplm.projectinventarisuas.data.model.User
import java.security.MessageDigest

class UserRepository {

    private val database = DatabaseProvider.getDatabaseReference()

    fun login(username: String, password: String, callback: (User?) -> Unit) {
        val hashedPassword = hashPassword(password)

        database.child("admin").get().addOnSuccessListener { adminSnapshot ->
            for (admin in adminSnapshot.children) {
                val adminUsername = admin.child("admin_username").value.toString()
                val adminPassword = admin.child("admin_password").value.toString()

                if (username == adminUsername && hashedPassword == adminPassword) {
                    val adminId = admin.child("admin_id").value.toString()
                    val adminName = admin.child("admin_name").value.toString()
                    val isPasswordChanged = admin.child("isPasswordChanged").value as Boolean
                    callback(User("admin", adminName, adminId, isPasswordChanged = isPasswordChanged))
                    return@addOnSuccessListener
                }
            }

            database.child("student").get().addOnSuccessListener { studentSnapshot ->
                for (student in studentSnapshot.children) {
                    val studentUsername = student.child("student_username").value.toString()
                    val studentPassword = student.child("student_password").value.toString()

                    if (username == studentUsername && hashedPassword == studentPassword) {
                        val studentId = student.child("student_id").value.toString()
                        val studentName = student.child("student_name").value.toString()
                        val isPasswordChanged = student.child("isPasswordChanged").value as Boolean
                        callback(
                            User(
                                "student",
                                studentName,
                                studentId,
                                isPasswordChanged = isPasswordChanged
                            )
                        )
                        return@addOnSuccessListener
                    }
                }
                callback(null)
            }.addOnFailureListener { callback(null) }

        }.addOnFailureListener { callback(null) }
    }

    fun changePassword(
        userId: String,
        userRole: String,
        newPassword: String,
        callback: (Boolean) -> Unit
    ) {
        val hashedPassword = hashPassword(newPassword)
        val userRef = database.child(userRole).child(userId)

        when (userRole) {
            "admin" -> {
                userRef.child("admin_password").setValue(hashedPassword)
            }

            "student" -> {
                userRef.child("student_password").setValue(hashedPassword)
            }
        }

        userRef.child("isPasswordChanged").setValue(true).addOnCompleteListener { changeTask ->
            callback(changeTask.isSuccessful)
        }.addOnFailureListener { exception ->
            println("Error updating password: ${exception.message}")
            callback(false)
        }
    }

    private fun hashPassword(password: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(password.toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
}