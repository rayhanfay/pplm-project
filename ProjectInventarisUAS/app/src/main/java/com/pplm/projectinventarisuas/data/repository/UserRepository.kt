package com.pplm.projectinventarisuas.data.repository

import android.util.Log
import com.pplm.projectinventarisuas.data.database.DatabaseProvider
import com.pplm.projectinventarisuas.data.model.User
import com.pplm.projectinventarisuas.data.dao.UserDao
import java.security.MessageDigest
import com.google.firebase.database.DatabaseReference

class UserRepository : UserDao {

    private val database: DatabaseReference = DatabaseProvider.getDatabaseReference()

    private fun hashPassword(password: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(password.toByteArray(Charsets.UTF_8))
        val hashedPassword = hashBytes.joinToString("") { "%02x".format(it) }
        Log.d("UserRepository", "Password di-hash. Panjang hash: ${hashedPassword.length}")
        return hashedPassword
    }

    private fun checkStudentLogin(
        username: String,
        hashedPassword: String,
        callback: (User?) -> Unit
    ) {
        Log.d("UserRepository", "Memeriksa login siswa untuk Username: $username")
        database.child("student").get().addOnSuccessListener { studentSnapshot ->
            if (studentSnapshot.exists()) {
                Log.d(
                    "UserRepository",
                    "Snapshot siswa ditemukan. Jumlah anak: ${studentSnapshot.childrenCount}"
                )
                for (student in studentSnapshot.children) {
                    val studentUsername = student.child("student_username").value.toString().trim()
                    val studentPassword = student.child("student_password").value.toString().trim()
                    val studentId = student.child("student_id").value.toString()
                    val studentName = student.child("student_name").value.toString()
                    val isPasswordChanged =
                        student.child("isPasswordChanged").value as? Boolean ?: false
                    Log.d(
                        "UserRepository",
                        "Membandingkan siswa: DB Username=$studentUsername, DB Password=$studentPassword"
                    )
                    if (username == studentUsername && hashedPassword == studentPassword) {
                        Log.d(
                            "UserRepository",
                            "Login siswa berhasil untuk ID: $studentId, Nama: $studentName"
                        )
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
                Log.d("UserRepository", "Tidak ada siswa yang cocok ditemukan.")
            } else {
                Log.w("UserRepository", "Path siswa tidak ada atau kosong.")
            }
            callback(null)
        }.addOnFailureListener { exception ->
            Log.e("UserRepository", "Gagal mengambil data siswa: ${exception.message}", exception)
            callback(null)
        }
    }

    override fun login(username: String, password: String, callback: (User?) -> Unit) {
        val hashedPassword = hashPassword(password)
        Log.d(
            "UserRepository",
            "Mencoba login untuk Username: $username, Hashed Password: ${hashedPassword.take(5)}..." // Amankan log
        )

        database.child("admin").get().addOnSuccessListener { adminSnapshot ->
            if (adminSnapshot.exists()) {
                Log.d(
                    "UserRepository",
                    "Snapshot admin ditemukan. Jumlah anak: ${adminSnapshot.childrenCount}"
                )
                for (admin in adminSnapshot.children) {
                    val adminUsername = admin.child("admin_username").value.toString().trim()
                    val adminPassword = admin.child("admin_password").value.toString().trim()
                    val adminId = admin.child("admin_id").value.toString()
                    val adminName = admin.child("admin_name").value.toString()
                    val isPasswordChanged =
                        admin.child("isPasswordChanged").value as? Boolean ?: false
                    Log.d(
                        "UserRepository",
                        "Membandingkan admin: DB Username=$adminUsername, DB Password=${
                            adminPassword.take(
                                5
                            )
                        }..."
                    )
                    if (username == adminUsername && hashedPassword == adminPassword) {
                        Log.d(
                            "UserRepository",
                            "Login admin berhasil untuk ID: $adminId, Nama: $adminName"
                        )
                        callback(
                            User(
                                "admin",
                                adminName,
                                adminId,
                                isPasswordChanged = isPasswordChanged
                            )
                        )
                        return@addOnSuccessListener
                    }
                }
                Log.d("UserRepository", "Tidak ada admin yang cocok ditemukan.")
            } else {
                Log.w("UserRepository", "Path admin tidak ada atau kosong.")
            }
            checkStudentLogin(username, hashedPassword, callback)

        }.addOnFailureListener { exception ->
            Log.e("UserRepository", "Gagal mengambil data admin: ${exception.message}", exception)
            callback(null)
        }
    }

    override fun changePassword(
        userId: String,
        userRole: String,
        newPassword: String,
        callback: (Boolean) -> Unit
    ) {
        val hashedPassword = hashPassword(newPassword)
        val userRef = database.child(userRole).child(userId)
        Log.d(
            "UserRepository",
            "Mengubah password untuk User ID: $userId, Peran: $userRole, Hashed New Password: ${
                hashedPassword.take(
                    5
                )
            }..."
        )

        when (userRole) {
            "admin" -> {
                userRef.child("admin_password").setValue(hashedPassword)
                    .addOnSuccessListener {
                        Log.d("UserRepository", "Password admin berhasil diatur.")
                    }
                    .addOnFailureListener { e ->
                        Log.e(
                            "UserRepository",
                            "Gagal mengatur password admin: ${e.message}",
                            e
                        )
                    }
            }

            "student" -> {
                userRef.child("student_password").setValue(hashedPassword)
                    .addOnSuccessListener {
                        Log.d("UserRepository", "Password siswa berhasil diatur.")
                    }
                    .addOnFailureListener { e ->
                        Log.e(
                            "UserRepository",
                            "Gagal mengatur password siswa: ${e.message}",
                            e
                        )
                    }
            }
        }

        userRef.child("isPasswordChanged").setValue(true).addOnCompleteListener { changeTask ->
            if (changeTask.isSuccessful) {
                Log.d("UserRepository", "Status isPasswordChanged berhasil diatur ke true.")
            } else {
                Log.e(
                    "UserRepository",
                    "Gagal mengatur status isPasswordChanged: ${changeTask.exception?.message}",
                    changeTask.exception
                )
            }
            callback(changeTask.isSuccessful)
        }.addOnFailureListener { exception ->
            Log.e(
                "UserRepository",
                "Error saat memperbarui password: ${exception.message}",
                exception
            )
            callback(false)
        }
    }

    override fun fetchCurrentPassword(
        userId: String,
        userRole: String,
        callback: (String?) -> Unit
    ) {
        val userRef = database.child(userRole).child(userId)
        Log.d(
            "UserRepository",
            "Mengambil password saat ini untuk User ID: $userId, Peran: $userRole"
        )
        userRef.get().addOnSuccessListener { snapshot ->
            val currentPassword = if (userRole == "admin") {
                snapshot.child("admin_password").value.toString().trim()
            } else {
                snapshot.child("student_password").value.toString().trim()
            }
            Log.d(
                "UserRepository",
                "Password saat ini diambil: ${currentPassword?.take(5)}..."
            )
            callback(currentPassword)
        }.addOnFailureListener { exception ->
            Log.e(
                "UserRepository",
                "Gagal mengambil password saat ini: ${exception.message}",
                exception
            )
            callback(null)
        }
    }

    override fun isPasswordSameAsCurrent(
        userId: String,
        userRole: String,
        newPassword: String,
        callback: (Boolean) -> Unit
    ) {
        Log.d(
            "UserRepository",
            "Memeriksa apakah password baru sama dengan yang lama untuk User ID: $userId, Peran: $userRole"
        )
        fetchCurrentPassword(userId, userRole) { currentPassword ->
            if (currentPassword != null) {
                val hashedNewPassword = hashPassword(newPassword)
                val isSame = (hashedNewPassword == currentPassword)
                Log.d("UserRepository", "Password baru (hashed) sama dengan yang lama: $isSame")
                callback(isSame)
            } else {
                Log.w(
                    "UserRepository",
                    "Tidak dapat mengambil password saat ini untuk perbandingan."
                )
                callback(false)
            }
        }
    }
}