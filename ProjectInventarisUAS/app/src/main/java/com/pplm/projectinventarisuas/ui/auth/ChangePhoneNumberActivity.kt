package com.pplm.projectinventarisuas.ui.auth

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.pplm.projectinventarisuas.R
import com.pplm.projectinventarisuas.data.repository.BorrowingRepository
import com.pplm.projectinventarisuas.data.repository.ItemRepository
import com.pplm.projectinventarisuas.data.repository.UserRepository
import com.pplm.projectinventarisuas.databinding.ActivityChangePhoneNumberBinding
import com.pplm.projectinventarisuas.ui.adminsection.AdminSectionActivity
import com.pplm.projectinventarisuas.ui.studentsection.StudentSectionActivity
import com.pplm.projectinventarisuas.utils.components.CustomDialog
import com.pplm.projectinventarisuas.utils.viewmodel.UserViewModel
import com.pplm.projectinventarisuas.utils.viewmodel.ViewModelFactory

class ChangePhoneNumberActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChangePhoneNumberBinding
    private lateinit var viewModel: UserViewModel
    private lateinit var userId: String
    private lateinit var userRole: String
    private lateinit var userName: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChangePhoneNumberBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViewModel()

        userId = intent.getStringExtra("userId") ?: ""
        userRole = intent.getStringExtra("userRole") ?: ""
        userName = intent.getStringExtra("userName") ?: ""

        binding.etPhoneNumber.inputType = InputType.TYPE_CLASS_PHONE
        binding.tvChangePhoneNumber.text = getString(R.string.change_phone_number_title, userName)

        setupButtonSave()
        setupObservers()
    }

    private fun setupButtonSave() {
        binding.btnSave.setOnClickListener {
            val phoneNumber = binding.etPhoneNumber.text.toString().trim()

            if (phoneNumber.isEmpty()) {
                binding.etPhoneNumber.error = getString(R.string.phone_number_empty_error)
                return@setOnClickListener
            }
            if (!phoneNumber.matches(Regex("^[0-9]{10,13}$"))) {
                binding.etPhoneNumber.error = getString(R.string.phone_number_invalid_error)
                return@setOnClickListener
            }

            CustomDialog.showLoading(this, "Menyimpan nomor telepon...")
            viewModel.updatePhoneNumber(userId, userRole, phoneNumber)
        }
    }

    private fun setupObservers() {
        viewModel.phoneNumberChangeResult.observe(this) { success ->
            CustomDialog.dismissLoading()
            if (success) {
                CustomDialog.success(
                    context = this,
                    title = getString(R.string.success),
                    message = getString(R.string.phone_number_update_success)
                ) {
                    val sharedPref = getSharedPreferences("LoginSession", MODE_PRIVATE)
                    with(sharedPref.edit()) {
                        putBoolean("isPhoneNumberSet", true)
                        apply()
                    }

                    val intent = when (userRole) {
                        "admin" -> Intent(this, AdminSectionActivity::class.java)
                        "student" -> Intent(this, StudentSectionActivity::class.java)
                        else -> null
                    }
                    intent?.let {
                        startActivity(it)
                        finish()
                    }
                }
            } else {
                CustomDialog.alert(
                    context = this,
                    title = getString(R.string.failed),
                    message = getString(R.string.phone_number_update_failed)
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