package com.pplm.projectinventarisuas.ui.auth

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.util.Log
import android.view.MotionEvent
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.pplm.projectinventarisuas.R
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
    private var isPasswordVisible = false

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
        setupShowPassword()
        setupObservers()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupShowPassword() {
        binding.etNewPassword.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                if (event.rawX >= (binding.etNewPassword.right - binding.etNewPassword.compoundDrawables[2].bounds.width())) {
                    val isCurrentPasswordVisible = binding.etNewPassword.transformationMethod is HideReturnsTransformationMethod
                    if (isCurrentPasswordVisible) {
                        binding.etNewPassword.transformationMethod = PasswordTransformationMethod.getInstance()
                        binding.etNewPassword.setCompoundDrawablesWithIntrinsicBounds(
                            0, 0, R.drawable.ic_eye_closed, 0
                        )
                    } else {
                        binding.etNewPassword.transformationMethod = HideReturnsTransformationMethod.getInstance()
                        binding.etNewPassword.setCompoundDrawablesWithIntrinsicBounds(
                            0, 0, R.drawable.ic_eye, 0
                        )
                    }
                    binding.etNewPassword.post {
                        binding.etNewPassword.setSelection(binding.etNewPassword.text.length)
                    }
                    return@setOnTouchListener true
                }
            }
            false
        }

        binding.etConfirmNewPassword.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                if (event.rawX >= (binding.etConfirmNewPassword.right - binding.etConfirmNewPassword.compoundDrawables[2].bounds.width())) {
                    val isCurrentPasswordVisible = binding.etConfirmNewPassword.transformationMethod is HideReturnsTransformationMethod
                    if (isCurrentPasswordVisible) {
                        binding.etConfirmNewPassword.transformationMethod = PasswordTransformationMethod.getInstance()
                        binding.etConfirmNewPassword.setCompoundDrawablesWithIntrinsicBounds(
                            0, 0, R.drawable.ic_eye_closed, 0
                        )
                    } else {
                        binding.etConfirmNewPassword.transformationMethod = HideReturnsTransformationMethod.getInstance()
                        binding.etConfirmNewPassword.setCompoundDrawablesWithIntrinsicBounds(
                            0, 0, R.drawable.ic_eye, 0
                        )
                    }
                    binding.etConfirmNewPassword.post {
                        binding.etConfirmNewPassword.setSelection(binding.etConfirmNewPassword.text.length)
                    }
                    return@setOnTouchListener true
                }
            }
            false
        }
    }

    private fun setupButtonSave() {
        binding.btnSave.setOnClickListener {
            val newPassword = binding.etNewPassword.text.toString().trim()
            val confirmPassword = binding.etConfirmNewPassword.text.toString().trim()

            var isValid = true

            if (newPassword.length < 8) {
                binding.etNewPassword.error = "Password minimal 8 karakter"
                isValid = false
            }

            if (newPassword.isEmpty()) {
                binding.etNewPassword.error = "Password baru tidak boleh kosong"
                isValid = false
            }

            if (confirmPassword.isEmpty()) {
                binding.etConfirmNewPassword.error = "Konfirmasi password tidak boleh kosong"
                isValid = false
            }

            if (!isValid) return@setOnClickListener

            if (newPassword != confirmPassword) {
                binding.etConfirmNewPassword.error = "Konfirmasi tidak cocok dengan password baru"
                return@setOnClickListener
            }

            viewModel.isPasswordSameAsCurrent(userId, userRole, newPassword) { isSame ->
                if (isSame) {
                    CustomDialog.alert(this, "Password Tidak Berubah!!","Password baru tidak boleh sama dengan yang lama")
                } else {
                    viewModel.changePassword(userId, userRole, newPassword)
                }
            }
        }
    }

    private fun setupObservers() {
        viewModel.passwordChangeResult.observe(this) { success ->
            if (success) {
                CustomDialog.success(
                    context = this,
                    title = getString(R.string.title_change_password),
                    message = getString(R.string.success_change_password)
                ) {
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
                CustomDialog.alert(
                    context = this,
                    title = getString(R.string.failed),
                    message = getString(R.string.failed_change_password)
                )
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
