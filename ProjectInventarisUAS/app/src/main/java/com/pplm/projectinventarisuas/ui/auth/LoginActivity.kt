package com.pplm.projectinventarisuas.ui.auth

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.MotionEvent
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.pplm.projectinventarisuas.R
import com.pplm.projectinventarisuas.data.repository.BorrowingRepository
import com.pplm.projectinventarisuas.data.repository.ItemRepository
import com.pplm.projectinventarisuas.data.repository.UserRepository
import com.pplm.projectinventarisuas.databinding.ActivityLoginBinding
import com.pplm.projectinventarisuas.ui.adminsection.AdminSectionActivity
import com.pplm.projectinventarisuas.ui.studentsection.StudentSectionActivity
import com.pplm.projectinventarisuas.utils.components.CustomDialog
import com.pplm.projectinventarisuas.utils.viewmodel.UserViewModel
import com.pplm.projectinventarisuas.utils.viewmodel.ViewModelFactory

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var viewModel: UserViewModel
    private var isPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViewModel()
        setupButtonLogin()
        setupShowPassword()
        setupObserver()
    }

    private fun setupButtonLogin() {
        binding.btnLogin.setOnClickListener {
            val username = binding.etUsername.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (username.isEmpty() || password.isEmpty()) {
                CustomDialog.alert(this, "Lengkapi username dan password")
            } else {
                viewModel.login(username, password)
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupShowPassword() {
        binding.etPassword.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                if (event.rawX >= (binding.etPassword.right - binding.etPassword.compoundDrawables[2].bounds.width())) {
                    isPasswordVisible = !isPasswordVisible
                    if (isPasswordVisible) {
                        binding.etPassword.inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                        binding.etPassword.setCompoundDrawablesWithIntrinsicBounds(
                            0, 0, R.drawable.ic_eye, 0
                        )
                    } else {
                        binding.etPassword.inputType =
                            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                        binding.etPassword.setCompoundDrawablesWithIntrinsicBounds(
                            0, 0, R.drawable.ic_eye_closed, 0
                        )
                    }
                    binding.etPassword.post {
                        binding.etPassword.setSelection(binding.etPassword.text.length)
                    }
                    return@setOnTouchListener true
                }
            }
            false
        }
    }

    private fun setupObserver() {
        viewModel.user.observe(this) { user ->
            if (user != null) {
                val sharedPref = getSharedPreferences("LoginSession", MODE_PRIVATE)
                with(sharedPref.edit()) {
                    putBoolean("isLoggedIn", true)
                    putString("userRole", user.role)
                    putString("userName", user.name)
                    Log.e("Auth", "User Name: ${user.name}")
                    user.id?.let { putString("studentId", it) }
                    apply()
                }

                if (!user.isPasswordChanged) {
                    CustomDialog.alert(this, "Ini adalah login pertama Anda. Silakan ganti password terlebih dahulu.") {
                        val intent = Intent(this, ChangePasswordActivity::class.java)
                        intent.putExtra("userId", user.id)
                        intent.putExtra("userRole", user.role)
                        startActivity(intent)
                        finish()
                    }
                } else {
                    val intent = when (user.role) {
                        "admin" -> Intent(this, AdminSectionActivity::class.java)
                        "student" -> Intent(this, StudentSectionActivity::class.java)
                        else -> null
                    }
                    intent?.let {
                        CustomDialog.alert(this, "Login berhasil") {
                            startActivity(it)
                            finish()
                        }
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
