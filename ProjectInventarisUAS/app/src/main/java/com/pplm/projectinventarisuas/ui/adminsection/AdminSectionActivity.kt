package com.pplm.projectinventarisuas.ui.adminsection

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import com.pplm.projectinventarisuas.R
import com.pplm.projectinventarisuas.databinding.ActivityAdminSectionBinding
import com.pplm.projectinventarisuas.ui.adminsection.borrowing.BorrowingFragment
import com.pplm.projectinventarisuas.ui.adminsection.borrowing.ScanReturnActivity
import com.pplm.projectinventarisuas.ui.adminsection.item.AddItemDialogFragment
import com.pplm.projectinventarisuas.ui.adminsection.item.ItemFragment
import com.pplm.projectinventarisuas.ui.auth.ChangePhoneNumberActivity
import com.pplm.projectinventarisuas.ui.auth.LoginActivity
import com.pplm.projectinventarisuas.utils.components.CustomDialog
import com.pplm.projectinventarisuas.utils.getGreetingWithName

class AdminSectionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminSectionBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminSectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        checkPhoneNumberSet()
        setupGreeting()
        setupDefaultFragment()
        setupBottomNavigation()
        setupFabButtons()
        setupLogoutButton()
    }

    private fun checkPhoneNumberSet() {
        val sharedPref = getSharedPreferences("LoginSession", MODE_PRIVATE)
        val userRole = sharedPref.getString("userRole", "")
        val userId = sharedPref.getString("studentId", "")
        val userName = sharedPref.getString("userName", "") ?: ""
        val isPhoneNumberSet = sharedPref.getBoolean("isPhoneNumberSet", false)

        if (userRole == "admin" && !isPhoneNumberSet) {
            CustomDialog.alert(
                context = this,
                title = getString(R.string.need_phone_number),
                message = getString(R.string.phone_number_required)
            ) {
                val intent = Intent(this, ChangePhoneNumberActivity::class.java)
                intent.putExtra("userId", userId)
                intent.putExtra("userRole", userRole)
                intent.putExtra("userName", userName)
                startActivity(intent)
                finish()
            }
        }
    }

    private fun setupGreeting() {
        val sharedPref = getSharedPreferences("LoginSession", MODE_PRIVATE)
        val userName = sharedPref.getString("userName", "") ?: ""
        val greeting = getGreetingWithName(userName)
        binding.tvTitle.text = greeting
    }

    private fun setupDefaultFragment() {
        supportFragmentManager.beginTransaction()
            .replace(binding.fragmentContainer.id, BorrowingFragment())
            .commit()
        binding.fabAddItem.hide()
        binding.fabScanReturn.show()
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.borrowing_menu -> {
                    supportFragmentManager.beginTransaction()
                        .replace(binding.fragmentContainer.id, BorrowingFragment())
                        .commit()
                    binding.fabAddItem.hide()
                    binding.fabScanReturn.show()
                    true
                }
                R.id.item_menu -> {
                    supportFragmentManager.beginTransaction()
                        .replace(binding.fragmentContainer.id, ItemFragment())
                        .commit()
                    binding.fabAddItem.show()
                    binding.fabScanReturn.hide()
                    true
                }
                else -> false
            }
        }
    }

    private fun setupFabButtons() {
        binding.fabAddItem.setOnClickListener {
            checkPhoneNumberAndProceed {
                val dialog = AddItemDialogFragment()
                dialog.show(supportFragmentManager, "AddItemDialog")
            }
        }

        binding.fabScanReturn.setOnClickListener {
            checkPhoneNumberAndProceed {
                val intent = Intent(this, ScanReturnActivity::class.java)
                startActivity(intent)
            }
        }
    }

    private fun setupLogoutButton() {
        binding.btnLogout.setOnClickListener {
            CustomDialog.confirm(
                context = this,
                message = getString(R.string.logout_message),
                onConfirm = {
                    getSharedPreferences("LoginSession", MODE_PRIVATE).edit { clear() }
                    startActivity(Intent(this, LoginActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    })
                    finish()
                }
            )
        }
    }

    private fun checkPhoneNumberAndProceed(onProceed: () -> Unit) {
        val sharedPref = getSharedPreferences("LoginSession", MODE_PRIVATE)
        val userRole = sharedPref.getString("userRole", "")
        val isPhoneNumberSet = sharedPref.getBoolean("isPhoneNumberSet", false)
        val userId = sharedPref.getString("studentId", "")
        val userName = sharedPref.getString("userName", "") ?: ""

        if (userRole == "admin" && !isPhoneNumberSet) {
            CustomDialog.alert(
                context = this,
                title = getString(R.string.need_phone_number),
                message = getString(R.string.phone_number_required)
            ) {
                val intent = Intent(this, ChangePhoneNumberActivity::class.java)
                intent.putExtra("userId", userId)
                intent.putExtra("userRole", userRole)
                intent.putExtra("userName", userName)
                startActivity(intent)
            }
        } else {
            onProceed()
        }
    }
}
