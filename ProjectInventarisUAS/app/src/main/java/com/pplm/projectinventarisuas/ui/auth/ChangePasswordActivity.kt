package com.pplm.projectinventarisuas.ui.auth

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.pplm.projectinventarisuas.data.repository.BorrowingRepository
import com.pplm.projectinventarisuas.data.repository.ItemRepository
import com.pplm.projectinventarisuas.data.repository.UserRepository
import com.pplm.projectinventarisuas.databinding.ActivityChangePasswordBinding
import com.pplm.projectinventarisuas.utils.viewmodel.UserViewModel
import com.pplm.projectinventarisuas.utils.viewmodel.ViewModelFactory
import com.pplm.projectinventarisuas.utils.components.CustomDialog

class ChangePasswordActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChangePasswordBinding
    private lateinit var viewModel: UserViewModel
    private lateinit var userId: String
    private lateinit var userRole: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChangePasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViewModel()

        userId = intent.getStringExtra("userId") ?: ""
        userRole = intent.getStringExtra("userRole") ?: ""

        Log.e("Auth", "User  ID: $userId")
        Log.e("Auth", "User  Role: $userRole")

        setupButtonSave()
        setupObservers()
    }

    private fun setupButtonSave() {
        binding.btnSave.setOnClickListener {
            val newPassword = binding.etNewPassword.text.toString().trim()
            val confirmPassword = binding.etConfirmNewPassword.text.toString().trim()

            if (newPassword.isEmpty() || confirmPassword.isEmpty()) {
                CustomDialog.alert(this, "Please fill in all fields")
            } else if (newPassword != confirmPassword) {
                CustomDialog.alert(this, "Passwords do not match")
            } else {
                viewModel.isPasswordSameAsCurrent(userId, userRole, newPassword) { isSame ->
                    if (isSame) {
                        CustomDialog.alert(this, "New password cannot be the same as the old password")
                    } else {
                        viewModel.changePassword(userId, userRole, newPassword)
                    }
                }
            }
        }
    }

    private fun setupObservers() {
        viewModel.passwordChangeResult.observe(this) { success ->
            if (success) {
                CustomDialog.alert(this, "Password changed successfully") {
                    val sharedPref = getSharedPreferences("LoginSession", MODE_PRIVATE)
                    with(sharedPref.edit()) {
                        putBoolean("isLoggedIn", false)
                        apply()
                    }
                    val intent = Intent(this, LoginActivity::class.java)
                    startActivity(intent)
                    finish()
                }
            } else {
                CustomDialog.alert(this, "Failed to change password")
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
