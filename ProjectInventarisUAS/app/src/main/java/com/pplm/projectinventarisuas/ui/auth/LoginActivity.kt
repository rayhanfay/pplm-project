package com.pplm.projectinventarisuas.ui.auth

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.pplm.projectinventarisuas.data.repository.BorrowingRepository
import com.pplm.projectinventarisuas.data.repository.ItemRepository
import com.pplm.projectinventarisuas.data.repository.UserRepository
import com.pplm.projectinventarisuas.ui.adminsection.AdminSectionActivity
import com.pplm.projectinventarisuas.ui.studentsection.StudentSectionActivity
import com.pplm.projectinventarisuas.databinding.ActivityLoginBinding
import com.pplm.projectinventarisuas.utils.viewmodel.UserViewModel
import com.pplm.projectinventarisuas.utils.viewmodel.ViewModelFactory
import com.pplm.projectinventarisuas.utils.components.CustomDialog

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var viewModel: UserViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViewModel()

        binding.btnLogin.setOnClickListener {
            val username = binding.etUsername.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (username.isEmpty() || password.isEmpty()) {
                CustomDialog.alert(this, "Lengkapi username dan password")
            } else {
                viewModel.login(username, password)
            }
        }

        viewModel.user.observe(this) { user ->
            if (user != null) {
                val sharedPref = getSharedPreferences("LoginSession", MODE_PRIVATE)
                with(sharedPref.edit()) {
                    putBoolean("isLoggedIn", true)
                    putString("userRole", user.role)
                    putString("userName", user.name)
                    user.id?.let { putString("studentId", it) }
                    apply()
                }

                val intent = when (user.role) {
                    "admin" -> Intent(this, AdminSectionActivity::class.java)
                    "student" -> Intent(this, StudentSectionActivity::class.java)
                    else -> null
                }

                intent?.let {
                    CustomDialog.alert(this, "Login berhasil sebagai ${user.role}") {
                        startActivity(it)
                        finish()
                    }
                }

            } else {
                CustomDialog.alert(this, "Username atau password salah")
            }
        }
    }

    private fun setupViewModel() {
        val factory = ViewModelFactory(
            ItemRepository(),
            BorrowingRepository(),
            UserRepository()
        )
        viewModel = ViewModelProvider(this, factory)[UserViewModel::class.java]
    }
}
